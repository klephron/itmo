package ru.itmo.zavar.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public abstract class Actor {
    private final Address address;
}
