package ru.itmo.zavar.brainstop.video.streaming.model;

import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.model.VideoRenditionEntity;

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

