package ru.itmo.zavar;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Route {
    private final List<Coordinates> stops;
}
