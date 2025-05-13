package ru.itmo.zavar.accountservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuthorityRequest {
    private List<String> authorities;
}
