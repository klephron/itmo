package ru.itmo.zavar.ticktokcoretest.dto;

import ru.itmo.zavar.ticktokcoretest.transcode.JobStage;

import java.util.UUID;

public record UploadJobViewDto(
        UUID jobId,
        JobStage stage,
        int percent,
        String message,
        String fileName
) {}
