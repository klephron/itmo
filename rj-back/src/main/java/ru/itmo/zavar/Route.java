package ru.itmo.zavar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

@Getter
public class Route<T> {
    private final List<T> stops;
    private boolean isCyclic;

    public Route(List<T> stops, boolean isCyclic) {
        this.stops = Collections.unmodifiableList(new ArrayList<T>(stops));
        this.isCyclic = isCyclic;
    }

    public Integer getNext(int index) {
        if (index >= this.getStops().size() - 1) {
            if (this.isCyclic()) {
                return 0;
            } else {
                return null;
            }
        } else {
            return index + 1;
        }
    }
}
