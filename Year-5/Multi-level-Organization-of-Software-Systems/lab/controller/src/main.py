import json
import signal
from http.server import BaseHTTPRequestHandler, HTTPServer
from threading import Thread
from typing import Any

ROUTES = [("/")]

httpd_port: int = 8080
httpd: HTTPServer


class HTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        if self.path.startswith("/devices"):
            self.__handle_devices()
            return

        self.__handle_unknown_route()

    def log_message(self, format: Any, *args: Any):
        return

    def __handle_devices(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)

        try:
            received_data = json.loads(post_data)
            print(self.path, received_data)
            response = {"msg": "ok"}
        except json.JSONDecodeError:
            response = {"error": "Invalid JSON"}

        self.wfile.write(json.dumps(response).encode("utf-8"))

    def __handle_unknown_route(self):
        self.send_response(404)
        self.end_headers()

        response = {"error": "unknown route"}

        self.wfile.write(json.dumps(response).encode("utf-8"))


def httpd_shutdown() -> None:
    global httpd
    httpd.shutdown()


def signal_terminate(*_: Any) -> None:
    thread = Thread(target=httpd_shutdown)
    thread.start()


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    server_address = ("", httpd_port)
    httpd = HTTPServer(server_address, HTTPRequestHandler)
    print(f"Starting HTTP server on port {httpd_port}...")
    httpd.serve_forever(poll_interval=0.5)
    print("Terminated HTTP server")
