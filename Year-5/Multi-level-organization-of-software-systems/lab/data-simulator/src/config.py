import json
from typing import Any

from device import Device
from value import Value, ValueConst, ValueExponential, ValueUniform


def __config_parse_value(elem: Any) -> Value:
    if not isinstance(elem, dict):
        raise ValueError("value is not dict", elem)

    if elem["type"] == "CONST":
        return ValueConst(float(elem["value"]))
    elif elem["type"] == "UNIFORM":
        return ValueUniform(float(elem["min"]), float(elem["max"]))
    elif elem["type"] == "EXPONENTIAL":
        return ValueExponential(float(elem.get("min", 0)), float(elem["mean"]))
    else:
        raise ValueError("unknown value", elem)


def __config_parse_values(elem: Any) -> dict[str, Value]:
    d = {}
    for k, v in elem.items():
        if not isinstance(k, str):
            raise ValueError("k is not str", k)
        d[k] = __config_parse_value(v)
    return d


def __config_parse_device(elem: dict[Any, Any]) -> Device:
    return Device(
        elem["id"],
        __config_parse_value(elem["interval"]),
        __config_parse_values(elem.get("values", {})),
    )


def config_load_devices(json_path: str) -> list[Device]:
    devices = []
    with open(json_path) as f:
        d = json.load(f)
        if isinstance(d, list):
            for e in d:
                devices.append(__config_parse_device(e))
    return devices
