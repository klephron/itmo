package ru.itmo.zavar.brainstop.common.api.error;

public record ApiFieldError(
        String field,
        String message,
        Object rejectedValue
) {
}
