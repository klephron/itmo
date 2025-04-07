package ru.itmo.zavar.accountservice.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("users")
@Getter
@Setter
public class UserEntity {
    @Id
    private String id;
    private String username;
    private String password;
    private List<String> authorities;
}
