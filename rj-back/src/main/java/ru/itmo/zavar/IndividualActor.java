package ru.itmo.zavar;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class IndividualActor extends Actor {
    private final String name;
    private final String surname;
    private final String patronymic;

    public IndividualActor(Address address, int bufferCapacity, String name, String surname, String patronymic) {
        super(address, bufferCapacity);
        this.name = name;
        this.surname = surname;
        this.patronymic = patronymic;
    }
}
