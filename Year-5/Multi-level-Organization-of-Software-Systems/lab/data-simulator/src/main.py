from collections import namedtuple
from threading import Timer
from typing import Any
from config import config_load_devices
from simulation import Simulation
from device import Device, Message
from sortedcontainers import SortedDict
import json
import os
import signal

from value import Value, ValueConst, ValueExponential, ValueLinear

InputParams = namedtuple("InputParams", ["devices_cnt", "devices_freq", "json_path"])

statistics = SortedDict()
statistics_timer: Timer
statistics_interval = 1


def statistics_print() -> None:
    global statistics_timer, statistics, statistics_interval

    for k, v in statistics.items():
        print(f"{k}: avg={v["sum"] / v["cnt"]}")
    print()

    statistics_timer = Timer(statistics_interval, statistics_print)
    statistics_timer.start()


def on_message(device: Device, message: Message, time: float) -> None:
    if device.id not in statistics:
        statistics[device.id] = {"cnt": 1, "sum": time, "last": time}
    else:
        stat = statistics[device.id]
        stat["cnt"] += 1
        stat["sum"] += time - stat["last"]
        stat["last"] = time

    print("[", time, "]", ": id=", device.id, ",", json.dumps(message), sep="")


def signal_siginit(*_: Any) -> None:
    simulation.terminate()
    print("simulation terminated")


def values_generic() -> dict[str, Value]:
    return {
        "a": ValueExponential(10),
        "b": ValueExponential(1),
        "c": ValueConst(3),
        "d": ValueLinear(0, 10),
    }


def devices_generic(cnt: int, freq: float) -> list[Device]:
    devices: list[Device] = []
    interval: float = 1 / freq
    for i in range(1, cnt + 1):
        devices.append(
            Device(
                f"device/{i}",
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

    try:
        devices_cnt = int(os.environ.get("SIMULATOR_DEVICES_CNT", "10"))
        devices_freq = float(os.environ.get("SIMULATOR_DEVICES_FREQ", "5"))
        json_path = os.environ.get("SIMULATOR_JSON_PATH")
    except ValueError:
        raise ValueError("Look in process_params() for valid argument passing")

    return InputParams(
        devices_cnt=devices_cnt, devices_freq=devices_freq, json_path=json_path
    )


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_siginit)

    params = process_params()

    if params.json_path is not None:
        devices = config_load_devices(params.json_path)
    else:
        devices = devices_generic(params.devices_cnt, params.devices_freq)

    simulation = Simulation(devices, on_message)
    simulation.start()

    # statistics_timer = Timer(statistics_interval, statistics_print)
    # statistics_timer.start()

    simulation.join()

    # statistics_timer.cancel()
