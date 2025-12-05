package ru.itmo.zavar.ticktokcoretest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank
        @Size(max = 1024)
        String text
) {}

