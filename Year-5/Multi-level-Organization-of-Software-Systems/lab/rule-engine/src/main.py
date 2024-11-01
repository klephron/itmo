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

from bson import timestamp
import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exceptions import AMQPConnectionError, ConnectionWrongStateError
from pymongo import MongoClient
from pymongo.synchronous.collection import Collection
from pymongo.synchronous.database import Database

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

params: InputParams

rule_ctx = RuleCtx()

mongo_client: MongoClient[Any]
mongo_database: Database[Any]
mongo_rules: Collection[Any]
mongo_connect_timeout = 0.5
mongo_index_created = False

rabbitmq_conn: pika.BlockingConnection | None = None
rabbitmq_channel: BlockingChannel | None = None
rabbitmq_queue = "controller2rule-engine"
rabbitmq_connect_timeout = 0.5
rabbitmq_consumer_tag: Any | None = None


def rabbitmq_controller_on_message(
    ch: BlockingChannel, method: Any, properties: Any, body: str | bytes
) -> None:
    if isinstance(body, bytes):
        body = body.decode("utf-8")

    try:
        received_data = json.loads(body)
    except json.JSONDecodeError:
        print("warn: json decode error")
        return

    if "device_id" not in received_data:
        print("warn: device_id is not present")
        return
    device_id = received_data["device_id"]

    if "time" not in received_data:
        print("warn: time is not present")
        return
    time = received_data["time"]

    if not (isinstance(time, float) or isinstance(time, int)):
        print("warn: time is not float or int")
        return

    if "data" not in received_data:
        print("warn: data is not present")
        return
    data = received_data["data"]

    rules = rule_ctx.get_for_device(
        device_id, data, datetime.datetime.fromtimestamp(time)
    )
    if len(rules) > 0:
        mongo_rules.insert_many(rules)


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


def signal_terminate(*_: Any) -> None:
    if rabbitmq_channel is not None and rabbitmq_consumer_tag is not None:
        rabbitmq_channel.basic_cancel(rabbitmq_consumer_tag)


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    params = process_params()

    mongo_client = MongoClient(
        params.mongo_url, username=params.mongo_username, password=params.mongo_password
    )
    mongo_database = mongo_client["mops"]
    mongo_rules = mongo_database["rules"]

    while not mongo_index_created:
        try:
            mongo_rules.create_index(
                {
                    "time": 1,
                }
            )
            mongo_index_created = True
        except BaseException as e:
            print(e)
            time.sleep(mongo_connect_timeout)

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

        except (ConnectionWrongStateError, AMQPConnectionError) as e:
            print(f"error: {repr(e)}")
            time.sleep(rabbitmq_connect_timeout)
        except gaierror as e:
            print(f"error: {repr(e)}")
            time.sleep(rabbitmq_connect_timeout)

    if rabbitmq_channel is None:
        print("error: rabbitmq_channel is None")
        exit(-1)

    rabbitmq_consumer_tag = rabbitmq_channel.basic_consume(
        queue=rabbitmq_queue,
        on_message_callback=rabbitmq_controller_on_message,
        auto_ack=True,
    )
    rabbitmq_channel.start_consuming()
    print("info: stopped rabbitmq consumer")

    if rabbitmq_conn is not None:
        rabbitmq_conn.close()
        print("info: closed rabbitmq connection")
