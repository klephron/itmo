from typing import Any
from simulation import Device, Message, Simulation
import json
import signal

from value import ValueConst, ValueExponential, ValueLinear

simulation: Simulation


def on_message(device: Device, message: Message, time: float) -> None:
    print("[", time, "]", ": id=", device.id, ",", json.dumps(message), sep="")


def signal_siginit(*_: Any) -> None:
    simulation.terminate()
    print("simulation terminated")


if __name__ == "__main__":
    signal.signal(signal.SIGINT, signal_siginit)

    simulation = Simulation(
        [
            Device(1, ValueExponential(1), {"a": ValueLinear(3, 6)}),
            Device(2, ValueLinear(0, 2), {"a": ValueLinear(3, 6)}),
            Device(3, ValueLinear(0, 2), {"a": ValueLinear(3, 6)}),
            Device(4, ValueConst(3), {"a": ValueLinear(3, 6)}),
            Device(5, ValueConst(3), {"a": ValueLinear(3, 6)}),
        ],
        on_message,
    )
    simulation.start()
    simulation.join()
    print("end")
