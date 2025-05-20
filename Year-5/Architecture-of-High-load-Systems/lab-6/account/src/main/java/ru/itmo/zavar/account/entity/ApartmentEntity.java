package ru.itmo.zavar.account.entity;

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
    @NotEmpty
    private String id;
    @NotNull
    @NotEmpty
    private String userId;
    @NotEmpty
    @NotNull
    private String name;
}
