package ru.itmo.zavar.accountservice.security;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Roles {
    USER("USER"),
    ADMIN("ADMIN"),
    TSJ("TSJ");
    private final String role;

    @Override
    public String toString() {
        return role;
    }

    public static boolean isValid(String value) {
        for (Roles r : Roles.values()) {
            if (r.toString().equals(value)) return true;
        }
        return false;
    }
}
