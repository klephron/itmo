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
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()

        content_length = int(self.headers["Content-Length"])
        post_data = self.rfile.read(content_length)

        try:
            received_data = json.loads(post_data)
            response = {"message": "Data received successfully", "data": received_data}
        except json.JSONDecodeError:
            response = {"error": "Invalid JSON"}

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
