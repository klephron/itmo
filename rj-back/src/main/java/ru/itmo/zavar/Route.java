package ru.itmo.zavar;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class Route<T> {
    private final List<T> stops;

    public Route(List<T> stops) {
        this.stops = Collections.unmodifiableList(stops);
    }

    boolean isCyclic() {
        if (stops.isEmpty()) {
            return false;
        }
        return stops.getFirst().equals(stops.getLast());
    }
}
