package ru.itmo.zavar.ticktokcoretest.common.security;

public enum Role {
    USER,
    MODERATOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
