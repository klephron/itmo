package ru.itmo.zavar.exception;

public class MailApplyFailedException extends RuntimeException {
    public MailApplyFailedException(String message) {
        super(message);
    }
}
