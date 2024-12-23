package ru.itmo.zavar;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public abstract class Actor {
    private final Actor.Address address;

    public record Address(String country, String city, String state, String street, String postalCode) {
    }
}
