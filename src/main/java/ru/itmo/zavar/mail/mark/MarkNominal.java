package ru.itmo.zavar.mail.mark;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum MarkNominal {
    N1(1),
    N10(10),
    N25(25),
    N50(50),
    N100(100),
    N150(150),
    N200(200),
    N250(250),
    N500(500);
    private final int value;

    MarkNominal(int value) {
        this.value = value;
    }

    public static List<Integer> toValuesList() {
        return Arrays.stream(values()).map(nominal -> nominal.value).sorted(Comparator.reverseOrder()).toList();
    }
}
