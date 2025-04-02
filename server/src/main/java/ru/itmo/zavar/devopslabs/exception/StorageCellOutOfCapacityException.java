package ru.itmo.zavar.devopslabs.exception;

public class StorageCellOutOfCapacityException extends RuntimeException {
    public StorageCellOutOfCapacityException(String message) {
        super(message);
    }
}
