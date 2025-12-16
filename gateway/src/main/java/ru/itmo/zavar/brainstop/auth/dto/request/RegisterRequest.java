package ru.itmo.zavar.brainstop.auth.dto.request;

public record RegisterRequest(String username, String email, String displayName, String password) {
}
