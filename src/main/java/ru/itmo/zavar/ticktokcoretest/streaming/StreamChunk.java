package ru.itmo.zavar.ticktokcoretest.streaming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

@Getter
@AllArgsConstructor
public class StreamChunk {
    private final FileMetadataEntity fileMetadataEntity;
    private final byte[] chunk;
}

