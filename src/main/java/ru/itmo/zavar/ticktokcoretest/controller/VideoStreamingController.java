package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;
import ru.itmo.zavar.ticktokcoretest.service.VideoStreamingService;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamRange;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.*;

@RestController
@RequestMapping("/streaming/video")
@RequiredArgsConstructor
public class VideoStreamingController {

    private final VideoStreamingService videoStreamingService;

    @Value("${streaming.chunk.default-size}")
    private long chunkDefaultSize;

    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> fetchChunk(
            @RequestHeader(value = RANGE, required = false) String range,
            @PathVariable UUID uuid
    ) throws Exception {

        StreamRange parsedRange = StreamRange.parseHttpRangeString(range, chunkDefaultSize);
        StreamChunk streamChunk = videoStreamingService.fetchChunk(uuid, parsedRange);
        FileMetadata meta = streamChunk.getFileMetadata();
        long fileSize = meta.getContentSize();

        long rangeStart = parsedRange.getRangeStart();
        long rangeEnd = parsedRange.getRangeEnd(fileSize);

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE, meta.getContentType());
        headers.add(ACCEPT_RANGES, "bytes");
        headers.add(CONTENT_LENGTH, getContentLengthHeader(parsedRange, fileSize));
        headers.add(CONTENT_RANGE, getContentRangeHeader(parsedRange, fileSize));

        return new ResponseEntity<>(streamChunk.getChunk(), headers, HttpStatus.PARTIAL_CONTENT);
    }

    @GetMapping("/list")
    public List<FileMetadata> getVideoList() {
        return videoStreamingService.getVideoList();
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> saveVideo(
            @RequestParam("video") MultipartFile video,
            @RequestParam("videoName") String videoName
    ) throws Exception {
        return ResponseEntity.ok(videoStreamingService.saveVideo(video, videoName));
    }

    @PostMapping("/delete/{uuid}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID uuid) throws Exception {
        videoStreamingService.deleteVideo(uuid);
        return ResponseEntity.ok().build();
    }

    private String getContentLengthHeader(StreamRange range, long fileSize) {
        long end = range.getRangeEnd(fileSize);
        return String.valueOf(end - range.getRangeStart() + 1);
    }

    private String getContentRangeHeader(StreamRange range, long fileSize) {
        long end = range.getRangeEnd(fileSize);
        return "bytes " + range.getRangeStart() + "-" + end + "/" + fileSize;
    }
}
