package ru.itmo.zavar.account.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuthorityRequest {
    private List<String> authorities;
}
