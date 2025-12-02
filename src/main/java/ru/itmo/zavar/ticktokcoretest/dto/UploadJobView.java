package ru.itmo.zavar.ticktokcoretest.dto;

import ru.itmo.zavar.ticktokcoretest.upload.JobStage;

import java.util.UUID;

public record UploadJobView(
        UUID jobId,
        JobStage stage,
        int percent,
        String message,
        String fileName
) {}
