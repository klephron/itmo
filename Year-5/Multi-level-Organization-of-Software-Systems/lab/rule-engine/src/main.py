import datetime
import json
import os
import signal
import time
from collections import namedtuple
from socket import gaierror
from typing import Any

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

    rules = RULE_CTX.get_for_device(
        device_id, data, datetime.datetime.fromtimestamp(time)
    )
    if len(rules) > 0:
        MONGO_RULES.insert_many(rules)


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
    global RABBITMQ_CHANNEL, RABBITMQ_CONSUMER_TAG

    if RABBITMQ_CHANNEL is not None and RABBITMQ_CONSUMER_TAG is not None:
        RABBITMQ_CHANNEL.basic_cancel(RABBITMQ_CONSUMER_TAG)


def main() -> None:
    global PARAMS
    global MONGO_CLIENT, MONGO_DATABASE, MONGO_RULES, MONGO_INDEX_CREATED, MONGO_CONNECT_TIMEOUT
    global RABBITMQ_CONN, RABBITMQ_CHANNEL, RABBITMQ_QUEUE, RABBITMQ_CONNECT_TIMEOUT, RABBITMQ_CONSUMER_TAG

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
    RABBITMQ_CHANNEL.start_consuming()
    print("info: stopped rabbitmq consumer")

    if RABBITMQ_CONN is not None:
        RABBITMQ_CONN.close()
        print("info: closed rabbitmq connection")


if __name__ == "__main__":
    main()
