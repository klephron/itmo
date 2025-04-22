package ru.itmo.zavar.accountservice.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pay_history")
@Getter
@Setter
public final class PayHistoryEntity {
    @Id
    @NotNull
    @NotEmpty
    private String id;
    @NotNull
    @NotEmpty
    private String billId;
    @NotNull
    @NotEmpty
    private String userId;
    @NotNull
    @Positive
    private Long timestamp;
}
