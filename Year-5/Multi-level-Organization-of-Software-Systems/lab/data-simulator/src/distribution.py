import random
import math


def distribution_const(value: float) -> float:
    return value


def distribution_uniform(rng: random.Random, min: float, max: float) -> float:
    return rng.uniform(min, max)


def distribution_exponential(rng: random.Random, min: float, mean: float) -> float:
    return min + (-math.log(rng.random())) * (mean - min)
