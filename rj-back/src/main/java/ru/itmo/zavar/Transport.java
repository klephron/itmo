package ru.itmo.zavar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

@Getter
public class Transport {
    private final double speed;

    private Route<PostOffice> route = null;
    private List<Mail> mails = new ArrayList<>();

    Transport(int size, double speed) {
        this.speed = speed;
        mails = Arrays.asList(new Mail[size]);
    }

    // TODO: add more complex logic
    void setRoute(Route<PostOffice> route) {
        this.route = route;
    }
}
