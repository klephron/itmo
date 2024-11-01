import os
import signal
from collections import namedtuple
from concurrent.futures import Future, ThreadPoolExecutor
from threading import Semaphore, Timer
from typing import Any

import requests
from sortedcontainers import SortedDict

from config import config_load_devices
from device import Device, Message
from simulation import Simulation
from value import Value, ValueConst, ValueExponential, ValueUniform

InputParams = namedtuple(
    "InputParams",
    [
        "devices_cnt",
        "devices_freq",
        "json_path",
        "delay_check_enable",
        "delay_check_iters",
        "delay_check_threshold",
        "statistics_enable",
        "statistics_interval",
        "controller_url",
    ],
)

params: InputParams

statistics = SortedDict()
statistics_timer: Timer | None = None

delay_last: float = 0
delay_iter: int = 0

pool = ThreadPoolExecutor()
print(f"info: thread pool workers: {pool._max_workers}")
pool_semaphore = Semaphore(pool._max_workers * 2)


def statistics_print() -> None:
    global params, statistics_timer, statistics

    print("statistics:")
    for k, v in statistics.items():
        print(f"  {k}: interval.average:\t{v["sum"] / v["cnt"] :.6f}")

    statistics_timer = Timer(params.statistics_interval, statistics_print)
    statistics_timer.start()


def on_message_post(url: str, json: Any) -> requests.Response | None:
    try:
        return requests.post(url, json=json)
    except (
        requests.exceptions.InvalidSchema,
        requests.exceptions.MissingSchema,
        requests.exceptions.ConnectionError,
        requests.exceptions.ConnectTimeout,
    ) as e:
        print(f"error: {e}")
    except BaseException as e:
        print(f"error: unknown {e}")
    return None


def on_message_response(future: Future[requests.Response | None]) -> None:
    response = future.result()
    if response is not None and response.status_code != 200:
        print(
            f"error: received code {response.status_code} with content '{response.content.decode("utf-8")}'"
        )
    pool_semaphore.release()


def on_message(device: Device, message: Message, time: float, delay: float) -> None:
    global params, delay_last, delay_iter

    if params.statistics_enable:
        if device.id not in statistics:
            statistics[device.id] = {"cnt": 1, "sum": time, "last": time}
        else:
            stat = statistics[device.id]
            stat["cnt"] += 1
            stat["sum"] += time - stat["last"]
            stat["last"] = time

    if params.delay_check_enable:
        delay_iter = (delay_iter + 1) % params.delay_check_iters

        if delay_iter == 0:
            delay_diff = delay - delay_last

            if delay > params.delay_check_threshold:
                print(
                    f"warn: current simulation delay {delay_last :.3f}+{delay_diff :.3f}"
                )

            delay_last = delay

    pool_semaphore.acquire()
    pool.submit(
        on_message_post, params.controller_url + f"/devices/{device.id}", message
    ).add_done_callback(on_message_response)


def values_generic() -> dict[str, Value]:
    return {
        "const_10": ValueConst(10),
        "uniform_0_10": ValueUniform(0, 10),
        "exponential_0_5": ValueExponential(0, 5),
        "exponential_10_5": ValueExponential(10, 5),
        "exponential_0_10": ValueExponential(0, 10),
    }


def devices_generic(cnt: int, freq: float) -> list[Device]:
    devices: list[Device] = []
    interval: float = 1 / freq
    for i in range(1, cnt + 1):
        devices.append(
            Device(
                f"{i}",
                ValueConst(interval),
                values_generic(),
            )
        )

    return devices


def process_params() -> InputParams:
    if (os.environ.get("SIMULATOR_JSON_PATH") is not None) and (
        os.environ.get("SIMULATOR_DEVICES_CNT") is not None
        or os.environ.get("SIMULATOR_DEVICES_FREQ") is not None
    ):
        raise ValueError(
            "set only SIMULATOR_JSON_PATH or (SIMULATOR_DEVICES_CNT,SIMULATOR_DEVICES_FREQ)"
        )

    if os.environ.get("SIMULATOR_CONTROLLER_URL") is None:
        raise ValueError("not set SIMULATOR_CONTROLLER_URL")

    try:
        devices_cnt = int(os.environ.get("SIMULATOR_DEVICES_CNT", "10"))
        devices_freq = float(os.environ.get("SIMULATOR_DEVICES_FREQ", "5"))
        json_path = os.environ.get("SIMULATOR_JSON_PATH")
        delay_check_enable = (
            os.environ.get("SIMULATOR_DELAY_CHECK_ENABLE", "true").lower() == "true"
        )
        delay_check_iters = int(os.environ.get("SIMULATOR_DELAY_CHECK_ITER", "1000"))
        delay_check_threshold = float(
            os.environ.get("SIMULATOR_DELAY_CHECK_THRESHOLD", "1")
        )
        statistics_enable = (
            os.environ.get("SIMULATOR_STATISTICS_ENABLE", "false").lower() == "true"
        )
        statistics_interval = float(
            os.environ.get("SIMULATOR_STATISTICS_INTERVAL", "1")
        )
        controller_url = os.environ.get("SIMULATOR_CONTROLLER_URL")

    except ValueError:
        raise ValueError("Look in process_params() for valid argument passing")

    return InputParams(
        devices_cnt=devices_cnt,
        devices_freq=devices_freq,
        json_path=json_path,
        delay_check_enable=delay_check_enable,
        delay_check_iters=delay_check_iters,
        delay_check_threshold=delay_check_threshold,
        statistics_enable=statistics_enable,
        statistics_interval=statistics_interval,
        controller_url=controller_url,
    )


def signal_terminate(*_: Any) -> None:
    global simulation, statistics_timer

    simulation.terminate()

    if statistics_timer is not None:
        statistics_timer.cancel()
        statistics_timer = None


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_terminate)
    signal.signal(signal.SIGTERM, signal_terminate)

    params = process_params()

    if params.json_path is not None:
        devices = config_load_devices(params.json_path)
    else:
        devices = devices_generic(params.devices_cnt, params.devices_freq)

    simulation = Simulation(devices, on_message)
    simulation.start()

    if params.statistics_enable:
        statistics_timer = Timer(params.statistics_interval, statistics_print)
        statistics_timer.start()

    simulation.join()

    if params.statistics_enable and statistics_timer is not None:
        statistics_timer.cancel()
