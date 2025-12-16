package ru.itmo.zavar.brainstop.common.api.transcoding.dto;

import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;

import java.util.UUID;

public record UploadJobViewDto(
        UUID jobId,
        TranscodingJobStage stage,
        int percent,
        String message,
        String fileName
) {
}
