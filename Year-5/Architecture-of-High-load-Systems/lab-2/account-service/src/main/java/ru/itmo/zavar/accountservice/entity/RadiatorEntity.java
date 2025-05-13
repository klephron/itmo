package ru.itmo.zavar.accountservice.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("radiator")
@Getter
@Setter
public final class RadiatorEntity {
    @Id
    @NotNull
    @NotEmpty
    private String id;
    @NotNull
    @PositiveOrZero
    private Long deviceId;
    @NotNull
    @PositiveOrZero
    private Double radiatorTemp;
    @NotNull
    @PositiveOrZero
    private Double roomTemp;
    @NotNull
    @PositiveOrZero
    private Long timestamp;
    @NotNull
    @PositiveOrZero
    private Long heat;
}
