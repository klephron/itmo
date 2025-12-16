package ru.itmo.zavar.brainstop.common.security;

public enum Role {
    USER,
    MODERATOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
