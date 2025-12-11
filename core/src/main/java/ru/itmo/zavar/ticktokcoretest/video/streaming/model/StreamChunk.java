package ru.itmo.zavar.ticktokcoretest.video.streaming.model;

import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.video.model.VideoRenditionEntity;

public record StreamChunk(FileMetadataEntity fileMetadata, VideoRenditionEntity rendition, byte[] chunk) {

    public long contentSize() {
        if (rendition != null) {
            return rendition.getContentSize();
        }
        return fileMetadata.getContentSize();
    }

    public String contentType() {
        if (rendition != null) {
            return rendition.getContentType();
        }
        return fileMetadata.getContentType();
    }
}

