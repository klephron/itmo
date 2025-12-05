package ru.itmo.zavar.ticktokcoretest.transcode;

public enum JobStage {
    UPLOADING,
    TRANSCODING,
    UPLOADING_TO_MINIO,
    DONE,
    ERROR
}

