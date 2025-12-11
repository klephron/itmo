package ru.itmo.zavar.ticktokcoretest.auth.dto.request;

public record RegisterRequest(String username, String email, String displayName, String password) {
}
