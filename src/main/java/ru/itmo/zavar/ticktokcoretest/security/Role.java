package ru.itmo.zavar.ticktokcoretest.security;

public enum Role {
    USER,
    MODERATOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
