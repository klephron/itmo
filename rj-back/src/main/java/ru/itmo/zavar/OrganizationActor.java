package ru.itmo.zavar;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class OrganizationActor extends Actor {
    private final String name;

    public OrganizationActor(Address address, int bufferCapacity, String name) {
        super(address, bufferCapacity);
        this.name = name;
    }
}
