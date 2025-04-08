package ru.itmo.zavar.accountservice.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("bills")
@Getter
@Setter
public class BillEntity {
    @Id
    @NotNull
    private String id;
    @NotNull
    private String userId;
    @NotNull
    @Positive
    private Long timestamp;
    @NotNull
    @PositiveOrZero
    private Long heat;
    @NotNull
    @PositiveOrZero
    private Long cost;
    @NotNull
    private Boolean payed;
}
