package ru.itmo.zavar;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public final class OrganizationActor extends Actor {

    private final String name;

    public OrganizationActor(Address address, String name) {
        super(address);
        this.name = name;
    }
}
