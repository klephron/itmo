from typing import Any
from datetime import datetime

Rule = dict[Any, Any]


class RuleCtx:
    def __init__(self) -> None:
        self.device_uniform_0_10_cnt_gt_2: dict[str, int] = {}
        pass

    def get_device(
        self, device_id: str, device_data: dict[Any, Any], time: datetime
    ) -> list[Rule]:
        rules: list[Rule] = []
        self.__uniform_0_10_cnt_gt_2(rules, device_id, device_data, time)
        self.__exponential_0_10_gt_30(rules, device_id, device_data, time)
        return rules

    def __uniform_0_10_cnt_gt_2(
        self,
        rules: list[Rule],
        device_id: str,
        device_data: dict[Any, Any],
        time: datetime,
    ) -> None:
        if "uniform_0_10" not in device_data:
            return

        value = device_data["uniform_0_10"]
        if value <= 2:
            self.device_uniform_0_10_cnt_gt_2[device_id] = 0
            return

        cnt = self.device_uniform_0_10_cnt_gt_2[device_id] = (
            self.device_uniform_0_10_cnt_gt_2.setdefault(device_id, 0) + 1
        )
        if cnt < 10:
            return

        rules.append(
            self.__rule(
                "devices",
                time,
                {
                    "device_id": device_id,
                    "attribute": "uniform_0_10",
                    "count": cnt,
                    "msg": "count_last(value) >= 10 where all value > 2",
                },
            )
        )

    def __exponential_0_10_gt_30(
        self,
        rules: list[Rule],
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
                "devices",
                time,
                {
                    "device_id": device_id,
                    "attribute": "exponential_0_10",
                    "value": value,
                    "msg": "value > 30",
                },
            )
        )

    def __rule(self, group: str, time: datetime, other: dict[Any, Any]) -> Rule:
        other["group"] = group
        other["time"] = time
        return other
