package ru.itmo.zavar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Transport {
    private final double speed;
    private List<Mail> mails;

    @Setter
    private Route route = null;
    private List<Mail> mailList = new ArrayList<>();

    Transport(int size, double speed) {
        this.speed = speed;
        mails = Arrays.asList(new Mail[size]);
    }
}
