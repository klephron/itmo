package ru.itmo.zavar.brainstop.common.api.transcoding;

public record TranscodedRenditionMetadata(int height, String storageKey, long size, String contentType) {
}
