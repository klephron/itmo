package ru.itmo.zavar.actor;

import lombok.Getter;

@Getter
public final class OrganizationActor extends Actor {

    private final String name;

    public OrganizationActor(Address address, String name) {
        super(address);
        this.name = name;
    }
}
