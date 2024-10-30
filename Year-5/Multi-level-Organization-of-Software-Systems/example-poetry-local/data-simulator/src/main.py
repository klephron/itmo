from model.data import Model


def fun1(a: int) -> Model:
    return Model(a)


if __name__ == "__main__":
    print("hello, world!")
    a: Model = fun1(4)
    print(a.x, a.y)
