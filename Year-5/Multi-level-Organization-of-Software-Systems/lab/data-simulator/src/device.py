from typing import Self
from value import Value

Message = dict[str, float]


class Device:
    id: int
    values: dict[str, Value]
    interval: Value

    def __init__(self, id: int, interval: Value, values: dict[str, Value]):
        self.id = id
        self.interval = interval
        self.values = values

    def get_interval(self) -> float:
        return self.interval.value()

    def get_message(self) -> Message:
        dct = {}
        for k, v in self.values.items():
            dct[k] = v.value()
        return dct

    def __lt__(self, other: Self) -> bool:
        return self.id < other.id
