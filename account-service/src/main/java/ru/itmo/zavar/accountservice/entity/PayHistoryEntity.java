package ru.itmo.zavar.accountservice.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pay_history")
@Getter
@Setter
public class PayHistoryEntity {
    @Id
    @NotNull
    private String id;
    @NotNull
    private String billId;
    @NotNull
    private String userId;
    @NotNull
    @Positive
    private Long timestamp;
}
