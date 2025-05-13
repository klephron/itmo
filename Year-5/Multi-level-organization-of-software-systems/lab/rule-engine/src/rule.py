from typing import Any
from datetime import datetime
from dataclasses import dataclass, asdict
import uuid
import json

Rule = dict[Any, Any]


@dataclass
class CumulativeRecord:
    cnt: int
    history: set[uuid.UUID]


class CumulativeRecordJSONEncoder(json.JSONEncoder):
    def default(self, o: Any) -> Any:
        if isinstance(o, CumulativeRecord):
            record_dict = asdict(o)
            record_dict["history"] = [str(uuid) for uuid in record_dict["history"]]
            return record_dict
        elif isinstance(o, uuid.UUID):
            return str(o)
        elif isinstance(o, set):
            return list(o)
        return super().default(o)


def decode_cumulative_record(dct: dict[Any, Any]) -> CumulativeRecord:
    if "cnt" in dct and "history" in dct:
        # Convert the list of UUID strings back to a set of UUIDs
        dct["history"] = {uuid.UUID(uuid_str) for uuid_str in dct["history"]}
        return CumulativeRecord(**dct)
    raise json.JSONDecodeError("cumulative decode error", "", 0)


class RuleCtx:
    def __init__(self) -> None:
        # int - device_cnt, set[uuid.UUID] already counted uuids
        self.device_uniform_0_10_cnt_gt_2: dict[str, CumulativeRecord] = {}
        pass

    def get_for_device(
        self,
        device_uuid: uuid.UUID,
        device_id: str,
        device_data: dict[Any, Any],
        time: datetime,
    ) -> list[Rule]:
        rules: list[Rule] = []
        self.__uniform_0_10_cnt_gt_2(rules, device_uuid, device_id, device_data, time)
        self.__exponential_0_10_gt_30(rules, device_uuid, device_id, device_data, time)
        return rules

    def __uniform_0_10_cnt_gt_2(
        self,
        rules: list[Rule],
        device_uuid: uuid.UUID,
        device_id: str,
        device_data: dict[Any, Any],
        time: datetime,
    ) -> None:
        if "uniform_0_10" not in device_data:
            return

        value = device_data["uniform_0_10"]
        if value <= 2:
            self.device_uniform_0_10_cnt_gt_2[device_id] = CumulativeRecord(0, set())
            return

        record = self.device_uniform_0_10_cnt_gt_2.setdefault(
            device_id, CumulativeRecord(0, set())
        )

        if device_uuid in record.history:
            print(f"info: uuid {device_uuid} already in {device_id} history")
            return

        record.cnt += 1
        record.history.add(device_uuid)

        if record.cnt < 10:
            return

        rules.append(
            self.__rule(
                device_uuid,
                "devices",
                "uniform_0_10",
                time,
                {
                    "device_id": device_id,
                    "count": record.cnt,
                    "msg": "count_last(value) >= 10 where all value > 2",
                },
            )
        )

    def __exponential_0_10_gt_30(
        self,
        rules: list[Rule],
        device_uuid: uuid.UUID,
        device_id: str,
        device_data: dict[Any, Any],
        time: datetime,
    ) -> None:
        if "exponential_0_10" not in device_data:
            return

        value = device_data["exponential_0_10"]
        if value <= 30:
            return

        rules.append(
            self.__rule(
                device_uuid,
                "devices",
                "exponential_0_10",
                time,
                {
                    "device_id": device_id,
                    "value": value,
                    "msg": "value > 30",
                },
            )
        )

    def __rule(
        self,
        obj_uuid: uuid.UUID,
        group: str,
        attribute: str,
        time: datetime,
        other: dict[Any, Any],
    ) -> Rule:
        other["_id"] = {"uuid": obj_uuid, "group": group, "attribute": attribute}
        other["time"] = time
        return other
