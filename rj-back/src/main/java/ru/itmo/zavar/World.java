package ru.itmo.zavar;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

@Getter
public class World {
    private final Set<PostOffice> postOffices = new HashSet<>();
    private final Set<Transport> transports = new HashSet<>();

    public static World example1() {
        World world = new World();
        return world;
    }
}
