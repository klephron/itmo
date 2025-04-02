package ru.itmo.zavar.devopslabs.exception;

public class NetworkOutOfCapacityException extends RuntimeException {
    public NetworkOutOfCapacityException(String message) {
        super(message);
    }
}
