package ru.itmo.zavar.exception;

public class MarksIsOverException extends RuntimeException {
    public MarksIsOverException(String message) {
        super(message);
    }
}
