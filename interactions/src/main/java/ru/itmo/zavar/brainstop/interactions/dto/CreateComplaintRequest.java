package ru.itmo.zavar.brainstop.interactions.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateComplaintRequest(
        @NotBlank String reason
) {
}
