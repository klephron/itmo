package ru.itmo.zavar.accountservice.entity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("radiator_to_apartment")
@Getter
@Setter
public class RadiatorToApartmentEntity {
    @NotNull
    @NotEmpty
    private String apartmentId;
    @NotNull
    @NotEmpty
    private String deviceId;
}
