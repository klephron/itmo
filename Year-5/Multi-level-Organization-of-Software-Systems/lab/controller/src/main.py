import datetime
import json
import os
import signal
import time
from collections import namedtuple
from http.server import BaseHTTPRequestHandler, HTTPServer
from socket import gaierror
from threading import Thread
from time import sleep
from typing import Any, Tuple
import asyncio

import pika
from pika.adapters import BlockingConnection
from pika.adapters.blocking_connection import BlockingChannel
from pika.exceptions import (
    AMQPChannelError,
    AMQPConnectionError,
    ChannelWrongStateError,
    ConnectionWrongStateError,
    NackError,
    StreamLostError,
)
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Summary, generate_latest
from pymongo import MongoClient
from pymongo.errors import ExecutionTimeout, ServerSelectionTimeoutError
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database

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

HTTPD_PORT: int = 8080
HTTPD: HTTPServer

MONGO_CLIENT: MongoClient[Any]
MONGO_DATABASE: Database[Any]
MONGO_DEVICES: Collection[Any]
MONGO_CONNECT_TIMEOUT = 0.5
MONGO_INSERT_TIMEOUT_DELAY = 0.1
MONGO_INSERT_RETRY_CNT = 10
MONGO_CONN_IS_AVAILABLE = False
MONGO_CONN_STARTED = False

MQ_CONN: pika.BlockingConnection | None = None
MQ_CHANNEL: BlockingChannel
MQ_RULE_ENGINE_QUEUE = "controller2rule-engine"
MQ_MONGO_DELAYED_QUEUE = "controller2mongo-delayed"
MQ_QUEUE_MAX_LENGTH = 1000
MQ_CONNECT_TIMEOUT = 0.5
MQ_CONN_IS_AVAILABLE = False
MQ_CONN_STARTED = False

PROMETHEUS_REQUEST_COUNTER = Counter(
    "request_count", "Total number of requests", ("path", "method", "status_code")
)
PROMETHEUS_RESPONSE_TIME = Summary(
    "response_time_seconds",
    "Summary for tracking request response times",
    ("path", "method", "status_code"),
)


class RequestException(Exception):
    def __init__(self, prefix: str, code: int, e: Exception, msg: str) -> None:
        self.prefix = prefix
        self.code = code
        self.e = e
        self.msg = msg
        super().__init__()


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        global MONGO_CONN_IS_AVAILABLE, MQ_CONN_IS_AVAILABLE

        start_time = datetime.datetime.now()
        path = None
        code = None

        if not MONGO_CONN_IS_AVAILABLE or not MQ_CONN_IS_AVAILABLE:
            self.send_response(503)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(
                json.dumps({"error": "resource is temporary unavailable"}).encode(
                    "utf-8"
                )
            )
        else:
            try:
                if self.path.startswith("/devices/"):
                    path, code = self.__handle_devices("/devices/")
                else:
                    path, code = self.__handle_unknown_route()
            except RequestException as e:
                path = e.prefix
                code = e.code
                print(f"error: {e.msg}")
                try:
                    raise e.e
                except ServerSelectionTimeoutError:
                    Thread(target=mongo_reconnect).start()
                except (AMQPChannelError, AMQPConnectionError):
                    Thread(target=mq_reconnect).start()

        if path is not None and code is not None:
            PROMETHEUS_REQUEST_COUNTER.labels(
                path=path, method="POST", status_code=code
            ).inc()

            PROMETHEUS_RESPONSE_TIME.labels(
                path=path, method="POST", status_code=code
            ).observe((datetime.datetime.now() - start_time).total_seconds())

    def do_GET(self) -> None:
        start_time = datetime.datetime.now()

        if self.path == "/metrics":
            path, code = self.__handle_metrics("/metrics")
        else:
            path, code = self.__handle_unknown_route()

        PROMETHEUS_REQUEST_COUNTER.labels(
            path=path, method="GET", status_code=code
        ).inc()

        PROMETHEUS_RESPONSE_TIME.labels(
            path=path, method="GET", status_code=code
        ).observe((datetime.datetime.now() - start_time).total_seconds())

    def log_message(self, format: Any, *args: Any) -> None:
        return

    def __handle_devices(self, prefix: str) -> Tuple[str, int]:
        global MONGO_DEVICES, MONGO_INSERT_TIMEOUT_DELAY, MQ_CHANNEL, MQ_RULE_ENGINE_QUEUE

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)
        device_id = self.path[len(prefix) :]

        data = None
        error = False
        time = None
        code = None
        response = None

        try:
            data = json.loads(post_data)
        except json.JSONDecodeError:
            response = {"error": "Invalid JSON"}
            code = 415
            error = True

        if not error:
            assert data is not None
            if not self.__validate_devices(data):
                response = {"error": "bad request"}
                code = 400
                error = True

        if not error:
            time = datetime.datetime.now()
            try:
                MQ_CHANNEL.basic_publish(
                    exchange="",
                    routing_key=MQ_RULE_ENGINE_QUEUE,
                    body=json.dumps(
                        {
                            "device_id": device_id,
                            "time": time.timestamp(),
                            "data": data,
                        }
                    ),
                )
            except NackError as _:
                response = {"error": "temporary unavailable"}
                self.send_header("Retry-After", "10")
                code = 503
                error = True
            except Exception as e:
                raise RequestException(prefix, 503, e, "can't publish to mq_channel")

        if not error:
            assert data is not None
            assert time is not None

            delay = MONGO_INSERT_TIMEOUT_DELAY
            max_delay = MONGO_INSERT_TIMEOUT_DELAY * MONGO_INSERT_RETRY_CNT
            document = self.__get_devices_data(device_id, time, data)

            while True:
                if delay > max_delay:
                    print(
                        f"warn: insert timeout exceeded {max_delay}, inserting in delayed queue"
                    )
                    try:
                        self.__publish_mongo_delayed(
                            self.__get_devices_data(device_id, time, data),
                            "timeout exceeded",
                        )
                        break
                    except Exception as e:
                        raise RequestException(
                            prefix,
                            503,
                            e,
                            "can't publish in mq_channel, system is desynced",
                        )

                try:
                    MONGO_DEVICES.insert_one(document)
                    break
                except ExecutionTimeout as e:
                    print(f"warn: execution timeout {repr(e)}")
                    sleep(delay)
                    delay += MONGO_INSERT_TIMEOUT_DELAY
                except Exception as e:
                    print("connection exception")
                    raise RequestException(prefix, 503, e, "can't connect to mongo")

        if not error:
            response = {"msg": "ok"}
            code = 200

        if code is None or response is None:
            code = 500
            response = {"error": "500"}

        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(response).encode("utf-8"))
        return (prefix, code)

    def __validate_devices(self, data: dict[Any, Any]) -> bool:
        for k, v in data.items():
            if not isinstance(k, str):
                return False
            if not (isinstance(v, float) or isinstance(v, int)):
                return False

        return True

    def __publish_mongo_delayed(self, data: dict[Any, Any], error: str) -> None:
        data["time"] = datetime.datetime.timestamp(data["time"])
        insert_data = {}
        insert_data["mongo_error"] = error
        insert_data["data"] = data
        MQ_CHANNEL.basic_publish(
            "", routing_key=MQ_MONGO_DELAYED_QUEUE, body=json.dumps(insert_data)
        )

    def __get_devices_data(
        self, device_id: str, time: datetime.datetime, data: dict[Any, Any]
    ) -> dict[Any, Any]:
        return {"device_id": device_id, "time": time, "data": data}

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


def process_params() -> InputParams:
    if os.environ.get("CONTROLLER_MONGO_URL") is None:
        raise ValueError("not set CONTROLLER_MONGO_URL")

    if os.environ.get("CONTROLLER_MQ_HOST") is None:
        raise ValueError("not set CONTROLLER_MQ_HOST")

    try:
        mongo_url = os.environ.get("CONTROLLER_MONGO_URL")
        mongo_username = os.environ.get("CONTROLLER_MONGO_USERNAME", "root")
        mongo_password = os.environ.get("CONTROLLER_MONGO_PASSWORD", "root")
        mq_host = os.environ.get("CONTROLLER_MQ_HOST")
        mq_username = os.environ.get("CONTROLLER_MQ_USERNAME", "root")
        mq_password = os.environ.get("CONTROLLER_MQ_PASSWORD", "root")

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


def signal_terminate(*_: Any) -> None:
    Thread(target=lambda: HTTPD.shutdown()).start()


def mongo_reconnect():
    global PARAMS
    global MONGO_INDEX_CREATED, MONGO_CONNECT_TIMEOUT
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_DEVICES, MONGO_CONN_IS_AVAILABLE, MONGO_CONN_STARTED

    if MONGO_CONN_STARTED:
        return

    MONGO_CONN_STARTED = True
    MONGO_CONN_IS_AVAILABLE = False

    print("info: establishing mongo connection...")
    client = MongoClient(
        PARAMS.mongo_url,
        username=PARAMS.mongo_username,
        password=PARAMS.mongo_password,
        connectTimeoutMS=1000,
        socketTimeoutMS=1000,
        serverSelectionTimeoutMS=1000,
    )
    database = client["mops"]
    devices = database["devices"]

    index_created = False
    while not index_created:
        try:
            devices.create_index({"time": 1})
            index_created = True
        except Exception as e:
            print(e)
            time.sleep(MONGO_CONNECT_TIMEOUT)

    MONGO_CLIENT, MONGO_DATABASE, MONGO_DEVICES = client, database, devices
    MONGO_CONN_IS_AVAILABLE = True
    MONGO_CONN_STARTED = False
    print("info: established mongo connection")


def mq_reconnect():
    global PARAMS
    global MQ_RULE_ENGINE_QUEUE, MQ_CONNECT_TIMEOUT, MQ_CONN, MQ_CHANNEL, MQ_CONN_STARTED, MQ_CONN_IS_AVAILABLE

    if MQ_CONN_STARTED:
        return

    MQ_CONN_STARTED = True
    MQ_CONN_IS_AVAILABLE = False

    print("info: establishing mqtt connection...")
    mq_conn = None
    mq_channel = None
    while mq_conn is None:
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
                queue=MQ_RULE_ENGINE_QUEUE,
                arguments={
                    "x-max-length": MQ_QUEUE_MAX_LENGTH,
                    "x-overflow": "reject-publish",
                },
            )
            mq_channel.queue_declare(
                queue=MQ_MONGO_DELAYED_QUEUE,
            )
            mq_channel.confirm_delivery()
        except (ConnectionWrongStateError, AMQPConnectionError) as e:
            print(f"error: {repr(e)}")
            time.sleep(MQ_CONNECT_TIMEOUT)
        except gaierror as e:
            print(f"error: {repr(e)}")
            time.sleep(MQ_CONNECT_TIMEOUT)

    assert mq_conn is not None
    assert mq_channel is not None

    MQ_CONN, MQ_CHANNEL = mq_conn, mq_channel
    MQ_CONN_IS_AVAILABLE = True
    MQ_CONN_STARTED = False
    print("info: established mqtt connection")


def main() -> None:
    global PARAMS
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_DEVICES, MONGO_CONN_IS_AVAILABLE
    global MQ_CONN, MQ_CHANNEL, MQ_CONN_IS_AVAILABLE
    global HTTPD, HTTPD_PORT

    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    PARAMS = process_params()

    mongo_reconnect()
    mq_reconnect()

    # HTTPD
    server_address = ("", HTTPD_PORT)
    HTTPD = HTTPServer(server_address, HTTPRequestHandler)
    print(f"info: starting HTTP server on port {HTTPD_PORT}...")
    HTTPD.serve_forever(poll_interval=0.5)
    print("info: terminated HTTP server")

    # cleanup
    if MQ_CONN is not None:
        MQ_CONN.close()
        print("info: closed mq connection")


if __name__ == "__main__":
    main()
