package ru.itmo.zavar.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class Actor {
    private final Address address;
}
