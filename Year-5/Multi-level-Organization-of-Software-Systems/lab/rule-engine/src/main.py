import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import os
import signal
from threading import Thread
import time
from collections import namedtuple
from socket import gaierror
from typing import Any, Tuple

import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exceptions import (
    AMQPConnectionError,
    ConnectionClosedByBroker,
    ConnectionWrongStateError,
)
from pymongo import MongoClient
from pymongo.errors import ServerSelectionTimeoutError
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest

from rule import RuleCtx

InputParams = namedtuple(
    "InputParams",
    [
        "mongo_url",
        "mongo_username",
        "mongo_password",
        "mq_host",
        "mq_username",
        "mq_password",
    ],
)

ROUTES = [("/")]

PARAMS: InputParams

RULE_CTX = RuleCtx()

MONGO_CLIENT: MongoClient[Any]
MONGO_DATABASE: Database[Any]
MONGO_RULES: Collection[Any]
MONGO_CONNECT_TIMEOUT = 0.5
MONGO_CONN_IS_AVAILABLE = False
MONGO_CONN_STARTED = False

MQ_CONN: pika.BlockingConnection
MQ_CHANNEL: BlockingChannel | None
MQ_QUEUE = "controller2rule-engine"
MQ_QUEUE_MAX_LENGTH = 1000
MQ_CONNECT_TIMEOUT = 0.5
MQ_CONSUMER_TAG: Any | None = None
MQ_CONN_IS_AVAILABLE = False
MQ_CONN_STARTED = False

HTTPD_PORT = 8080
HTTPD: HTTPServer

PROMETHEUS_REQUEST_COUNTER = Counter(
    "request_count", "Total number of requests", ("path", "method", "status_code")
)
PROMETHEUS_MQ_REQUEST_COUNTER = Counter(
    "mq_request_count", "Total number of mq requests", ("queue", "status")
)
PROMETHEUS_MQ_RESPONSE_TIME_HIST = Histogram(
    "mq_response_time_seconds",
    "Histogram for tracking request response times",
    ("queue", "status"),
    buckets=(
        0.00001,
        0.000025,
        0.0001,
        0.00025,
        0.001,
        0.0025,
        0.01,
        0.025,
        0.1,
        0.25,
        1.0,
    ),
)


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        if self.path == "/metrics":
            path, code = self.__handle_metrics("/metrics")
        else:
            path, code = self.__handle_unknown_route()

        PROMETHEUS_REQUEST_COUNTER.labels(
            path=path, method="GET", status_code=code
        ).inc()

    def log_message(self, format: Any, *args: Any) -> None:
        return

    def __handle_metrics(self, prefix: str) -> Tuple[str, int]:
        code = 200
        self.send_response(code)
        self.send_header("Content-Type", CONTENT_TYPE_LATEST)
        self.end_headers()
        self.wfile.write(generate_latest())
        return (prefix, code)

    def __handle_unknown_route(self) -> Tuple[str, int]:
        code = 404
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"error": "unknown route"}).encode("utf-8"))
        return ("/404", code)


def mq_device_parse(
    body: bytes | str,
) -> Tuple[bool, Tuple[str, float, dict[Any, Any]] | None]:
    if isinstance(body, bytes):
        body = body.decode("utf-8")

    try:
        received_data = json.loads(body)
    except json.JSONDecodeError:
        print("warn: json decode error")
        return (False, None)

    if "device_id" not in received_data:
        print("warn: device_id is not present")
        return (False, None)

    device_id = str(received_data["device_id"])

    if "time" not in received_data:
        print("warn: time is not present")
        return (False, None)
    time = received_data["time"]

    if not (isinstance(time, float) or isinstance(time, int)):
        print("warn: time is not float or int")
        return (False, None)
    time = float(time)

    if "data" not in received_data:
        print("warn: data is not present")
        return (False, None)

    data = received_data["data"]
    if not isinstance(data, dict):
        return (False, None)

    return (True, (device_id, time, data))


def mq_controller_on_message(
    ch: BlockingChannel, method: Any, properties: Any, body: str | bytes
) -> None:
    start_time = datetime.datetime.now()
    ok, parse_data = mq_device_parse(body)

    if ok and parse_data is not None:
        device_id, time, data = parse_data
        rules = RULE_CTX.get_for_device(
            device_id, data, datetime.datetime.fromtimestamp(time)
        )
        # try insert to mongo after connection is established
        ok = False
        if len(rules) > 0:
            while not ok:
                try:
                    MONGO_RULES.insert_many(rules)
                    ok = True
                except ServerSelectionTimeoutError as e:
                    print(f"error: {repr(e)}, trying to reconnect...")
                    mongo_reconnect()

    ch.basic_ack(delivery_tag=method.delivery_tag)

    status = "OK" if ok else "BAD_FORMAT"

    PROMETHEUS_MQ_REQUEST_COUNTER.labels(queue=MQ_QUEUE, status=status).inc()

    PROMETHEUS_MQ_RESPONSE_TIME_HIST.labels(queue=MQ_QUEUE, status=status).observe(
        (datetime.datetime.now() - start_time).total_seconds()
    )


def process_params() -> InputParams:
    if os.environ.get("RULE_ENGINE_MONGO_URL") is None:
        raise ValueError("not set RULE_ENGINE_MONGO_URL")

    if os.environ.get("RULE_ENGINE_MQ_HOST") is None:
        raise ValueError("not set RULE_ENGINE_MQ_HOST")

    try:
        mongo_url = os.environ.get("RULE_ENGINE_MONGO_URL")
        mongo_username = os.environ.get("RULE_ENGINE_MONGO_USERNAME", "root")
        mongo_password = os.environ.get("RULE_ENGINE_MONGO_PASSWORD", "root")
        mq_host = os.environ.get("RULE_ENGINE_MQ_HOST")
        mq_username = os.environ.get("RULE_ENGINE_MQ_USERNAME", "root")
        mq_password = os.environ.get("RULE_ENGINE_MQ_PASSWORD", "root")

    except ValueError:
        raise ValueError("Look in process_params() for valid argument passing")

    return InputParams(
        mongo_url=mongo_url,
        mongo_username=mongo_username,
        mongo_password=mongo_password,
        mq_host=mq_host,
        mq_username=mq_username,
        mq_password=mq_password,
    )


def mongo_reconnect() -> None:
    global PARAMS
    global MONGO_INDEX_CREATED, MONGO_CONNECT_TIMEOUT
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_RULES, MONGO_CONN_IS_AVAILABLE, MONGO_CONN_STARTED

    if MONGO_CONN_STARTED:
        return

    MONGO_CONN_STARTED = True
    MONGO_CONN_IS_AVAILABLE = False

    print("info: establishing mongo connection...")
    client: MongoClient[Any] = MongoClient(
        PARAMS.mongo_url,
        username=PARAMS.mongo_username,
        password=PARAMS.mongo_password,
        connectTimeoutMS=1000,
        socketTimeoutMS=1000,
        serverSelectionTimeoutMS=1000,
    )
    database = client["mops"]
    rules = database["rules"]

    index_created = False
    while not index_created:
        try:
            rules.create_index({"time": 1})
            index_created = True
        except Exception as e:
            print(e)
            time.sleep(MONGO_CONNECT_TIMEOUT)

    MONGO_CLIENT, MONGO_DATABASE, MONGO_RULES = client, database, rules
    MONGO_CONN_IS_AVAILABLE = True
    MONGO_CONN_STARTED = False
    print("info: established mongo connection")


def mq_reconnect() -> None:
    global PARAMS
    global MQ_RULE_ENGINE_QUEUE, MQ_CONNECT_TIMEOUT
    global MQ_CONN, MQ_CHANNEL, MQ_CONN_STARTED, MQ_CONN_IS_AVAILABLE, MQ_CONSUMER_TAG

    if MQ_CONN_STARTED:
        return

    MQ_CONN_STARTED = True
    MQ_CONN_IS_AVAILABLE = False

    print("info: establishing mqtt connection...")
    mq_conn = None
    mq_channel = None

    while mq_channel is None:
        try:
            mq_conn = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=PARAMS.mq_host,
                    credentials=pika.PlainCredentials(
                        PARAMS.mq_username, PARAMS.mq_password
                    ),
                ),
            )
            mq_channel = mq_conn.channel()
            mq_channel.queue_declare(
                queue=MQ_QUEUE,
                arguments={
                    "x-max-length": MQ_QUEUE_MAX_LENGTH,
                    "x-overflow": "reject-publish",
                },
            )
            mq_channel.confirm_delivery()
        except (ConnectionWrongStateError, AMQPConnectionError) as e:
            print(f"error: {repr(e)}")
            time.sleep(MQ_CONNECT_TIMEOUT)
        except gaierror as e:
            print(f"error: {repr(e)}")
            time.sleep(MQ_CONNECT_TIMEOUT)

    mq_consumer_tag = mq_channel.basic_consume(
        queue=MQ_QUEUE, on_message_callback=mq_controller_on_message, auto_ack=False
    )

    assert mq_conn is not None
    assert mq_channel is not None

    MQ_CONN, MQ_CHANNEL, MQ_CONSUMER_TAG = mq_conn, mq_channel, mq_consumer_tag
    MQ_CONN_IS_AVAILABLE = True
    MQ_CONN_STARTED = False
    print("info: established mqtt connection")


def serve_httpd() -> None:
    global HTTPD, HTTPD_PORT

    server_address = ("", HTTPD_PORT)
    HTTPD = HTTPServer(server_address, HTTPRequestHandler)
    print(f"info: starting HTTP server on port {HTTPD_PORT}...")
    HTTPD.serve_forever(poll_interval=0.5)
    print("info: terminated HTTP server")


def signal_terminate(*_: Any) -> None:
    global HTTPD, MQ_CHANNEL, MQ_CONSUMER_TAG

    httpd_thread = Thread(target=lambda: HTTPD.shutdown())
    httpd_thread.start()

    if MQ_CHANNEL is not None and MQ_CONSUMER_TAG is not None:
        MQ_CHANNEL.basic_cancel(MQ_CONSUMER_TAG)
        MQ_CHANNEL.close()
        MQ_CHANNEL = None
        print("info: closed mq connection")

    httpd_thread.join()


def main() -> None:
    global PARAMS
    global MQ_CONN, MQ_CHANNEL, MQ_QUEUE, MQ_CONNECT_TIMEOUT, MQ_CONSUMER_TAG
    global HTTPD, HTTPD_PORT
    global PROMETHEUS_HTTP_PORT

    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    PARAMS = process_params()

    mongo_reconnect()
    mq_reconnect()

    assert MQ_CHANNEL is not None

    print(f"info: created basic_consume {MQ_QUEUE}")

    Thread(target=serve_httpd).start()

    while MQ_CHANNEL is not None:
        try:
            print("info: starting consuming...")
            MQ_CHANNEL.start_consuming()
            print("info: stopped mq consumer")
        except ConnectionClosedByBroker as e:
            print(f"error: {repr(e)}")
            mq_reconnect()


if __name__ == "__main__":
    main()
