import random
from typing import Callable

from distribution import distribution_const, distribution_exp, distribution_linear


class Value:
    rng: random.Random
    func: Callable[[random.Random], float]

    def __init__(
        self, seed: int | None, func: Callable[[random.Random], float]
    ) -> None:
        self.rng = random.Random(seed)
        self.func = func

    def value(self) -> float:
        return self.func(self.rng)


class ValueConst(Value):
    def __init__(self, value: float) -> None:
        super().__init__(None, lambda _: distribution_const(value))


class ValueLinear(Value):
    def __init__(self, a: float, b: float, seed: int | None = None) -> None:
        super().__init__(seed, lambda rng: distribution_linear(rng, a, b))


class ValueExponential(Value):
    def __init__(self, a: float, seed: int | None = None) -> None:
        super().__init__(seed, lambda rng: distribution_exp(rng, a))
