import datetime
import json
import os
import signal
import time
from collections import namedtuple
from http.server import BaseHTTPRequestHandler, HTTPServer
from socket import gaierror
from threading import Thread
from typing import Any, Tuple

import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exceptions import AMQPConnectionError, ConnectionWrongStateError
from pymongo import MongoClient
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database
from prometheus_client import CONTENT_TYPE_LATEST, Summary, generate_latest, Counter

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
MONGO_INDEX_CREATED = False
MONGO_CONNECT_TIMEOUT = 0.5

MQ_CONN: pika.BlockingConnection | None = None
MQ_CHANNEL: BlockingChannel
MQ_QUEUE = "controller2rule-engine"
MQ_CONNECT_TIMEOUT = 0.5

PROMETHEUS_REQUEST_COUNTER = Counter(
    "request_count", "Total number of requests", ("path", "method", "status_code")
)
PROMETHEUS_RESPONSE_TIME = Summary(
    "response_time_seconds",
    "Summary for tracking request response times",
    ("path", "method", "status_code"),
)


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        start_time = datetime.datetime.now()

        if self.path.startswith("/devices/"):
            path, code = self.__handle_devices("/devices/")
        else:
            path, code = self.__handle_unknown_route()

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
        global MONGO_DEVICES, MQ_CHANNEL, MQ_QUEUE

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)
        device_id = self.path[len(prefix) :]

        code = 200

        try:
            received_data = json.loads(post_data)

            if not self.__validate_devices(received_data):
                response = {"error": "bad request"}
                code = 400
            else:
                time = datetime.datetime.now()
                MONGO_DEVICES.insert_one(
                    {"id": device_id, "time": time, "data": received_data}
                )

                MQ_CHANNEL.basic_publish(
                    exchange="",
                    routing_key=MQ_QUEUE,
                    body=json.dumps(
                        {
                            "device_id": device_id,
                            "time": time.timestamp(),
                            "data": received_data,
                        }
                    ),
                )

                response = {"msg": "ok"}

        except json.JSONDecodeError:
            response = {"error": "Invalid JSON"}
            code = 415

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


def main() -> None:
    global PARAMS
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_DEVICES, MONGO_INDEX_CREATED, MONGO_CONNECT_TIMEOUT
    global MQ_CONN, MQ_CHANNEL, MQ_QUEUE, MQ_CONNECT_TIMEOUT
    global HTTPD, HTTPD_PORT

    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    PARAMS = process_params()

    MONGO_CLIENT = MongoClient(
        PARAMS.mongo_url, username=PARAMS.mongo_username, password=PARAMS.mongo_password
    )
    MONGO_DATABASE = MONGO_CLIENT["mops"]
    MONGO_DEVICES = MONGO_DATABASE["devices"]
    while not MONGO_INDEX_CREATED:
        try:
            MONGO_DEVICES.create_index(
                {
                    "time": 1,
                }
            )
            MONGO_INDEX_CREATED = True
        except BaseException as e:
            print(e)
            time.sleep(MONGO_CONNECT_TIMEOUT)

    while MQ_CONN is None:
        try:
            MQ_CONN = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=PARAMS.mq_host,
                    credentials=pika.PlainCredentials(
                        PARAMS.mq_username, PARAMS.mq_password
                    ),
                ),
            )
            MQ_CHANNEL = MQ_CONN.channel()
            MQ_CHANNEL.queue_declare(queue=MQ_QUEUE)
        except (ConnectionWrongStateError, AMQPConnectionError) as e:
            print(f"error: {repr(e)}")
            time.sleep(MQ_CONNECT_TIMEOUT)
        except gaierror as e:
            print(f"error: {repr(e)}")
            time.sleep(MQ_CONNECT_TIMEOUT)

    server_address = ("", HTTPD_PORT)
    HTTPD = HTTPServer(server_address, HTTPRequestHandler)

    print(f"info: starting HTTP server on port {HTTPD_PORT}...")
    HTTPD.serve_forever(poll_interval=0.5)
    print("info: terminated HTTP server")

    if MQ_CONN is not None:
        MQ_CONN.close()
        print("info: closed mq connection")


if __name__ == "__main__":
    main()
