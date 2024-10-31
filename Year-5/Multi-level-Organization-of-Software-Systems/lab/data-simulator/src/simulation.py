from collections.abc import Callable
from queue import PriorityQueue
from threading import Timer
from typing import Tuple

from device import Device, Message


class Simulation:
    queue: PriorityQueue[Tuple[float, Device]]
    devices: list[Device]
    on_message: Callable[[Device, Message, float], None]
    is_running: bool
    current_time: float
    timer: Timer | None

    def __init__(
        self,
        devices: list[Device],
        on_message: Callable[[Device, Message, float], None],
    ) -> None:
        self.queue = PriorityQueue()
        self.devices = devices
        self.on_message = on_message
        self.is_running = False

    def start(self) -> None:
        for device in self.devices:
            interval = device.get_interval()
            self.queue.put((interval, device))

        self.is_running = True
        self.current_time = 0
        self.__continue()

    def stop(self, wait: bool | None = True) -> None:
        self.is_running = False
        if wait:
            self.join()

    def join(self) -> None:
        self.queue.join()

    def terminate(self) -> None:
        self.is_running = False
        if self.timer is not None:
            self.timer.cancel()

        while self.queue.qsize() != 0:
            self.queue.get_nowait()
            self.queue.task_done()

    def __continue(self) -> None:
        if self.queue.qsize() != 0:
            self.timer = Timer(self.__next_timeout(), self.__next)
            self.timer.start()

    def __next(self) -> None:
        current_time, device = self.queue.get()
        message = device.get_message()

        self.current_time = current_time
        self.on_message(device, message, self.current_time)
        self.queue.task_done()

        if self.is_running:
            next_time = current_time + device.get_interval()
            self.queue.put((next_time, device))

        self.__continue()

    def __next_timeout(self) -> float:
        return self.queue.queue[0][0] - self.current_time
