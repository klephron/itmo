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
from pika.exceptions import AMQPConnectionError, ConnectionWrongStateError
from pymongo import MongoClient
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database
from prometheus_client import CONTENT_TYPE_LATEST, Counter, generate_latest

from rule import RuleCtx

InputParams = namedtuple(
    "InputParams",
    [
        "mongo_url",
        "mongo_username",
        "mongo_password",
        "rabbitmq_host",
        "rabbitmq_username",
        "rabbitmq_password",
    ],
)

ROUTES = [("/")]

PARAMS: InputParams

RULE_CTX = RuleCtx()

MONGO_CLIENT: MongoClient[Any]
MONGO_DATABASE: Database[Any]
MONGO_RULES: Collection[Any]
MONGO_CONNECT_TIMEOUT = 0.5
MONGO_INDEX_CREATED = False

RABBITMQ_CONN: pika.BlockingConnection | None = None
RABBITMQ_CHANNEL: BlockingChannel | None = None
RABBITMQ_QUEUE = "controller2rule-engine"
RABBITMQ_CONNECT_TIMEOUT = 0.5
RABBITMQ_CONSUMER_TAG: Any | None = None

HTTPD_PORT = 8080
HTTPD: HTTPServer

PROMETHEUS_REQUEST_COUNTER = Counter(
    "request_count", "Total number of requests", ("path", "method", "status_code")
)
PROMETHEUS_RABBITMQ_REQUEST_COUNTER = Counter(
    "rabbitmq_request_count", "Total number of rabbitmq requests", ("queue", "status")
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


def rabbitmq_device_parse(
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


def rabbitmq_controller_on_message(
    ch: BlockingChannel, method: Any, properties: Any, body: str | bytes
) -> None:
    ok, parse_data = rabbitmq_device_parse(body)

    if not ok or parse_data is None:
        PROMETHEUS_RABBITMQ_REQUEST_COUNTER.labels(
            queue=RABBITMQ_QUEUE, status="BAD_FORMAT"
        ).inc()
        return

    device_id, time, data = parse_data
    rules = RULE_CTX.get_for_device(
        device_id, data, datetime.datetime.fromtimestamp(time)
    )
    if len(rules) > 0:
        MONGO_RULES.insert_many(rules)

    PROMETHEUS_RABBITMQ_REQUEST_COUNTER.labels(queue=RABBITMQ_QUEUE, status="OK").inc()


def process_params() -> InputParams:
    if os.environ.get("RULE_ENGINE_MONGO_URL") is None:
        raise ValueError("not set RULE_ENGINE_MONGO_URL")

    if os.environ.get("RULE_ENGINE_RABBITMQ_HOST") is None:
        raise ValueError("not set RULE_ENGINE_RABBITMQ_HOST")

    try:
        mongo_url = os.environ.get("RULE_ENGINE_MONGO_URL")
        mongo_username = os.environ.get("RULE_ENGINE_MONGO_USERNAME", "root")
        mongo_password = os.environ.get("RULE_ENGINE_MONGO_PASSWORD", "root")
        rabbitmq_host = os.environ.get("RULE_ENGINE_RABBITMQ_HOST")
        rabbitmq_username = os.environ.get("RULE_ENGINE_RABBITMQ_USERNAME", "root")
        rabbitmq_password = os.environ.get("RULE_ENGINE_RABBITMQ_PASSWORD", "root")

    except ValueError:
        raise ValueError("Look in process_params() for valid argument passing")

    return InputParams(
        mongo_url=mongo_url,
        mongo_username=mongo_username,
        mongo_password=mongo_password,
        rabbitmq_host=rabbitmq_host,
        rabbitmq_username=rabbitmq_username,
        rabbitmq_password=rabbitmq_password,
    )


def serve_httpd() -> None:
    global HTTPD, HTTPD_PORT

    server_address = ("", HTTPD_PORT)
    HTTPD = HTTPServer(server_address, HTTPRequestHandler)
    print(f"info: starting HTTP server on port {HTTPD_PORT}...")
    HTTPD.serve_forever(poll_interval=0.5)
    print("info: terminated HTTP server")


def signal_terminate(*_: Any) -> None:
    global HTTPD, RABBITMQ_CHANNEL, RABBITMQ_CONSUMER_TAG

    httpd_thread = Thread(target=lambda: HTTPD.shutdown())
    httpd_thread.start()

    if RABBITMQ_CHANNEL is not None and RABBITMQ_CONSUMER_TAG is not None:
        RABBITMQ_CHANNEL.basic_cancel(RABBITMQ_CONSUMER_TAG)

    httpd_thread.join()


def main() -> None:
    global PARAMS
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_RULES, MONGO_INDEX_CREATED, MONGO_CONNECT_TIMEOUT
    global RABBITMQ_CONN, RABBITMQ_CHANNEL, RABBITMQ_QUEUE, RABBITMQ_CONNECT_TIMEOUT, RABBITMQ_CONSUMER_TAG
    global HTTPD, HTTPD_PORT
    global PROMETHEUS_HTTP_PORT

    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    PARAMS = process_params()

    MONGO_CLIENT = MongoClient(
        PARAMS.mongo_url, username=PARAMS.mongo_username, password=PARAMS.mongo_password
    )
    MONGO_DATABASE = MONGO_CLIENT["mops"]
    MONGO_RULES = MONGO_DATABASE["rules"]

    while not MONGO_INDEX_CREATED:
        try:
            MONGO_RULES.create_index(
                {
                    "time": 1,
                }
            )
            MONGO_INDEX_CREATED = True
        except BaseException as e:
            print(e)
            time.sleep(MONGO_CONNECT_TIMEOUT)

    while RABBITMQ_CONN is None:
        try:
            RABBITMQ_CONN = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=PARAMS.rabbitmq_host,
                    credentials=pika.PlainCredentials(
                        PARAMS.rabbitmq_username, PARAMS.rabbitmq_password
                    ),
                ),
            )
            RABBITMQ_CHANNEL = RABBITMQ_CONN.channel()
            RABBITMQ_CHANNEL.queue_declare(queue=RABBITMQ_QUEUE)

        except (ConnectionWrongStateError, AMQPConnectionError) as e:
            print(f"error: {repr(e)}")
            time.sleep(RABBITMQ_CONNECT_TIMEOUT)
        except gaierror as e:
            print(f"error: {repr(e)}")
            time.sleep(RABBITMQ_CONNECT_TIMEOUT)

    if RABBITMQ_CHANNEL is None:
        print("error: rabbitmq_channel is None")
        exit(-1)

    RABBITMQ_CONSUMER_TAG = RABBITMQ_CHANNEL.basic_consume(
        queue=RABBITMQ_QUEUE,
        on_message_callback=rabbitmq_controller_on_message,
        auto_ack=True,
    )
    print(f"info: created basic_consume {RABBITMQ_QUEUE}")

    Thread(target=serve_httpd).start()

    print("info: starting consuming...")
    RABBITMQ_CHANNEL.start_consuming()
    print("info: stopped rabbitmq consumer")

    if RABBITMQ_CONN is not None:
        RABBITMQ_CONN.close()
        print("info: closed rabbitmq connection")


if __name__ == "__main__":
    main()
