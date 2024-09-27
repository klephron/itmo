package ru.itmo.zavar.actor;

import lombok.Getter;

@Getter
public final class IndividualActor extends Actor {

    private final String name;
    private final String surname;
    private final String patronymic;

    public IndividualActor(String name, String surname, String patronymic, Address address) {
        super(address);
        this.name = name;
        this.surname = surname;
        this.patronymic = patronymic;
    }
}
