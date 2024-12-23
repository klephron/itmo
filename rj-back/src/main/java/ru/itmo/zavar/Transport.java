package ru.itmo.zavar;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class Transport {
    private final long mailCapacity;
    private final double speed;

    @Setter
    private Route route = null;
    private List<Mail> mailList = new ArrayList<>();

    Transport() {
        mailCapacity = 100;
        speed = 10;
    }
}
