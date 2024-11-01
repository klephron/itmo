import json
import signal
from http.server import BaseHTTPRequestHandler, HTTPServer
from threading import Thread
from typing import Any
from collections import namedtuple
import os
from pymongo import MongoClient
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database
import datetime
from rule import RuleCtx

InputParams = namedtuple(
    "InputParams",
    ["mongo_url", "mongo_username", "mongo_password"],
)

ROUTES = [("/")]

params: InputParams

httpd_port: int = 8080
httpd: HTTPServer

rule_ctx = RuleCtx()

mongo_client: MongoClient[Any]
mongo_database: Database[Any]
mongo_rules: Collection[Any]


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        if self.path.startswith("/devices/"):
            self.__handle_devices("/devices/")
            return

        self.__handle_unknown_route()

    def log_message(self, format: Any, *args: Any) -> None:
        return

    def __handle_devices(self, prefix: str) -> None:
        global mongo_rules

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)

        device_id = self.path[len(prefix) :]

        try:
            received_data = json.loads(post_data)

            rules = rule_ctx.get_device(
                device_id, received_data, datetime.datetime.now()
            )
            if len(rules) > 0:
                mongo_rules.insert_many(rules)

            response = {"msg": "ok"}
        except json.JSONDecodeError:
            response = {"error": "Invalid JSON"}

        self.wfile.write(json.dumps(response).encode("utf-8"))

    def __handle_unknown_route(self) -> None:
        self.send_response(404)
        self.end_headers()

        response = {"error": "unknown route"}

        self.wfile.write(json.dumps(response).encode("utf-8"))


def process_params() -> InputParams:
    if os.environ.get("RULE_ENGINE_MONGO_URL") is None:
        raise ValueError("not set RULE_ENGINE_MONGO_URL")

    try:
        mongo_url = os.environ.get("RULE_ENGINE_MONGO_URL")
        mongo_username = os.environ.get("RULE_ENGINE_MONGO_USERNAME", "root")
        mongo_password = os.environ.get("RULE_ENGINE_MONGO_PASSWORD", "root")

    except ValueError:
        raise ValueError("Look in process_params() for valid argument passing")

    return InputParams(
        mongo_url=mongo_url,
        mongo_username=mongo_username,
        mongo_password=mongo_password,
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
    mongo_rules = mongo_database["rules"]
    mongo_rules.create_index(
        {
            "time": 1,
        }
    )

    server_address = ("", httpd_port)
    httpd = HTTPServer(server_address, HTTPRequestHandler)
    print(f"Starting HTTP server on port {httpd_port}...")
    httpd.serve_forever(poll_interval=0.5)
    print("Terminated HTTP server")
