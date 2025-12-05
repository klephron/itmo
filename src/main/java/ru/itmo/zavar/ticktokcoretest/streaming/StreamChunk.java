package ru.itmo.zavar.ticktokcoretest.streaming;

import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

public record StreamChunk(FileMetadataEntity fileMetadataEntity, byte[] chunk) {
}

