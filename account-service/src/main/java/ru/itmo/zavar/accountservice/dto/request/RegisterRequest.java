package ru.itmo.zavar.accountservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterRequest {
    private String username;
    private String password;
    private List<String> authorities;
}