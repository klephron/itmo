import datetime
import json
import os
import signal
import time
from collections import namedtuple
from http.server import BaseHTTPRequestHandler, HTTPServer
from socket import gaierror
from threading import Thread
from typing import Any

import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exceptions import AMQPConnectionError, ConnectionWrongStateError
from pymongo import MongoClient
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database

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

HTTPD_PORT: int = 8080
HTTPD: HTTPServer

MONGO_CLIENT: MongoClient[Any]
MONGO_DATABASE: Database[Any]
MONGO_DEVICES: Collection[Any]
MONGO_INDEX_CREATED = False
MONGO_CONNECT_TIMEOUT = 0.5

RABBITMQ_CONN: pika.BlockingConnection | None = None
RABBITMQ_CHANNEL: BlockingChannel
RABBITMQ_QUEUE = "controller2rule-engine"
RABBITMQ_CONNECT_TIMEOUT = 0.5


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        if self.path.startswith("/devices/"):
            self.__handle_devices("/devices/")
            return

        self.__handle_unknown_route()

    def log_message(self, format: Any, *args: Any) -> None:
        return

    def __handle_devices(self, prefix: str) -> None:
        global MONGO_DEVICES

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)
        device_id = self.path[len(prefix) :]

        try:
            received_data = json.loads(post_data)

            if not self.__validate_devices(received_data):
                response = {"error": "bad request"}
                self.send_response(400)
            else:
                time = datetime.datetime.now()
                MONGO_DEVICES.insert_one(
                    {"id": device_id, "time": time, "data": received_data}
                )

                RABBITMQ_CHANNEL.basic_publish(
                    exchange="",
                    routing_key=RABBITMQ_QUEUE,
                    body=json.dumps(
                        {
                            "device_id": device_id,
                            "time": time.timestamp(),
                            "data": received_data,
                        }
                    ),
                )

                response = {"msg": "ok"}
                self.send_response(200)

        except json.JSONDecodeError:
            response = {"error": "Invalid JSON"}
            self.send_response(415)

        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(response).encode("utf-8"))

    def __handle_unknown_route(self) -> None:
        self.send_header("Content-Type", "application/json")
        self.send_response(404)
        self.end_headers()
        self.wfile.write(json.dumps({"error": "unknown route"}).encode("utf-8"))

    def __validate_devices(self, data: dict[Any, Any]) -> bool:
        for k, v in data.items():
            if not isinstance(k, str):
                return False
            if not (isinstance(v, float) or isinstance(v, int)):
                return False

        return True


def process_params() -> InputParams:
    if os.environ.get("CONTROLLER_MONGO_URL") is None:
        raise ValueError("not set CONTROLLER_MONGO_URL")

    if os.environ.get("CONTROLLER_RABBITMQ_HOST") is None:
        raise ValueError("not set CONTROLLER_RABBITMQ_HOST")

    try:
        mongo_url = os.environ.get("CONTROLLER_MONGO_URL")
        mongo_username = os.environ.get("CONTROLLER_MONGO_USERNAME", "root")
        mongo_password = os.environ.get("CONTROLLER_MONGO_PASSWORD", "root")
        rabbitmq_host = os.environ.get("CONTROLLER_RABBITMQ_HOST")
        rabbitmq_username = os.environ.get("CONTROLLER_RABBITMQ_USERNAME", "root")
        rabbitmq_password = os.environ.get("CONTROLLER_RABBITMQ_PASSWORD", "root")

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


def httpd_shutdown() -> None:
    global HTTPD
    HTTPD.shutdown()


def signal_terminate(*_: Any) -> None:
    thread = Thread(target=httpd_shutdown)
    thread.start()

def main() -> None:
    global PARAMS
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_DEVICES, MONGO_INDEX_CREATED, MONGO_CONNECT_TIMEOUT
    global RABBITMQ_CONN, RABBITMQ_CHANNEL, RABBITMQ_QUEUE, RABBITMQ_CONNECT_TIMEOUT
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

    server_address = ("", HTTPD_PORT)
    HTTPD = HTTPServer(server_address, HTTPRequestHandler)

    print(f"info: starting HTTP server on port {HTTPD_PORT}...")
    HTTPD.serve_forever(poll_interval=0.5)
    print("info: terminated HTTP server")

    if RABBITMQ_CONN is not None:
        RABBITMQ_CONN.close()
        print("info: closed rabbitmq connection")


if __name__ == "__main__":
    main()
