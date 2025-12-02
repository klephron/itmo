package ru.itmo.zavar.ticktokcoretest.upload;

public enum JobStage {
    UPLOADING,
    TRANSCODING,
    UPLOADING_TO_MINIO,
    DONE,
    ERROR
}

