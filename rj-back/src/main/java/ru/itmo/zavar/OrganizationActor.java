package ru.itmo.zavar;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class OrganizationActor extends Actor {

    private final String name;

    public OrganizationActor(Actor.Address address, String name) {
        super(address);
        this.name = name;
    }
}
