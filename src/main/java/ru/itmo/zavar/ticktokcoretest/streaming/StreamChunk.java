package ru.itmo.zavar.ticktokcoretest.streaming;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;

@Getter
@AllArgsConstructor
public class StreamChunk {
    private final FileMetadata fileMetadata;
    private final byte[] chunk;
}

