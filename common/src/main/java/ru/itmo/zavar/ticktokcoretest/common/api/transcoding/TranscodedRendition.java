package ru.itmo.zavar.ticktokcoretest.common.api.transcoding;

import java.nio.file.Path;

public record TranscodedRendition(int height, Path path, long size) {
}
