import random
import math


def distribution_const(value: float) -> float:
    return value


def distribution_linear(rng: random.Random, a: float, b: float) -> float:
    return rng.uniform(a, b)


def distribution_exp(rng: random.Random, a: float) -> float:
    return -math.log(rng.random()) / a
