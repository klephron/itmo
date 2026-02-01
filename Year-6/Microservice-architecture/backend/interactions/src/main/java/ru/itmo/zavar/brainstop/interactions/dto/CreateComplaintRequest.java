package ru.itmo.zavar.brainstop.interactions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        @NotBlank
        @Size(max = 2000)
        String reason
) {
}
