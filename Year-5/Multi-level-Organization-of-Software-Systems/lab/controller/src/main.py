import json
import signal
from http.server import BaseHTTPRequestHandler, HTTPServer
from threading import Thread
from typing import Any
from collections import namedtuple
import os
from pika.adapters.blocking_connection import BlockingChannel
from pymongo import MongoClient
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database
import time
from socket import gaierror
from pika.exceptions import ConnectionWrongStateError
import pika

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

params: InputParams

httpd_port: int = 8080
httpd: HTTPServer

mongo_client: MongoClient[Any]
mongo_database: Database[Any]
mongo_devices: Collection[Any]

rabbitmq_conn: pika.BlockingConnection | None = None
rabbitmq_channel: BlockingChannel
rabbitmq_queue = "controller2rule-engine"
rabbitmq_connected: bool = False
rabbitmq_connect_timeout = 0.5


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        if self.path.startswith("/devices/"):
            self.__handle_devices("/devices/")
            return

        self.__handle_unknown_route()

    def log_message(self, format: Any, *args: Any) -> None:
        return

    def __handle_devices(self, prefix: str) -> None:
        global mongo_devices

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)
        device_id = self.path[len(prefix) :]

        try:
            received_data = json.loads(post_data)

            if not self.__validate_devices(received_data):
                response = {"error": "bad request"}
                self.send_response(400)
            else:
                mongo_devices.insert_one({"id": device_id, "data": received_data})

                rabbitmq_channel.basic_publish(
                    exchange="",
                    routing_key=rabbitmq_queue,
                    body=json.dumps({"device_id": device_id, "data": received_data}),
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
    global httpd
    httpd.shutdown()


def signal_terminate(*_: Any) -> None:
    thread = Thread(target=httpd_shutdown)
    thread.start()


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    params = process_params()

    mongo_client = MongoClient(
        params.mongo_url, username=params.mongo_username, password=params.mongo_password
    )
    mongo_database = mongo_client["mops"]
    mongo_devices = mongo_database["devices"]

    rabbitmq_conn = None
    while rabbitmq_conn is None:
        try:
            rabbitmq_conn = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=params.rabbitmq_host,
                    credentials=pika.PlainCredentials(
                        params.rabbitmq_username, params.rabbitmq_password
                    ),
                ),
            )
            rabbitmq_channel = rabbitmq_conn.channel()
            rabbitmq_channel.queue_declare(queue=rabbitmq_queue)
        except ConnectionWrongStateError as e:
            print(f"error: {repr(e)}")
            time.sleep(rabbitmq_connect_timeout)
        except gaierror as e:
            print(f"error: {repr(e)}")
            time.sleep(rabbitmq_connect_timeout)

    server_address = ("", httpd_port)
    httpd = HTTPServer(server_address, HTTPRequestHandler)

    print(f"Starting HTTP server on port {httpd_port}...")
    httpd.serve_forever(poll_interval=0.5)
    print("Terminated HTTP server")

    if rabbitmq_conn is not None:
        rabbitmq_conn.close()
        print("Closed rabbitmq connection")
