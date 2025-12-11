package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

public enum TranscodingJobStage {
    UPLOADING,
    TRANSCODING,
    UPLOADING_TO_MINIO,
    DONE,
    ERROR
}
