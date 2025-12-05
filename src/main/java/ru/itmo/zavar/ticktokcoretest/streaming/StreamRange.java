package ru.itmo.zavar.ticktokcoretest.streaming;

import lombok.Getter;

@Getter
public class StreamRange {

    private final long rangeStart;
    private final Long rangeEnd;   // может быть null, если до конца
    private final long defaultChunkSize;

    private StreamRange(long rangeStart, Long rangeEnd, long defaultChunkSize) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.defaultChunkSize = defaultChunkSize;
    }

    public static StreamRange parseHttpRangeString(String rangeHeader, long defaultChunkSize) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return new StreamRange(0L, null, defaultChunkSize);
        }

        String value = rangeHeader.substring("bytes=".length()).trim();
        String[] parts = value.split("-");
        long start = Long.parseLong(parts[0].trim());
        Long end = null;
        if (parts.length > 1 && !parts[1].isBlank()) {
            end = Long.parseLong(parts[1].trim());
        }

        return new StreamRange(start, end, defaultChunkSize);
    }

    public long getRangeEnd(long fileSize) {
        if (this.rangeEnd != null) {
            return Math.min(this.rangeEnd, fileSize - 1);
        }
        long candidate = this.rangeStart + this.defaultChunkSize - 1;
        return Math.min(candidate, fileSize - 1);
    }
}
