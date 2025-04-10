package ru.itmo.zavar.accountservice.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("apartments")
@Getter
@Setter
public final class ApartmentEntity {
    @Id
    @NotNull
    private String id;
    private String userId;
    @NotEmpty
    @NotNull
    private String name;
}
