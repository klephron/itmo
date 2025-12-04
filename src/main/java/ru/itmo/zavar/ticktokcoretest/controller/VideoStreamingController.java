package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.zavar.ticktokcoretest.dto.UploadJobView;
import ru.itmo.zavar.ticktokcoretest.integrations.s3.MinioService;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepo;
import ru.itmo.zavar.ticktokcoretest.service.VideoStreamingService;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamRange;
import ru.itmo.zavar.ticktokcoretest.upload.UploadJobRegistry;
import ru.itmo.zavar.ticktokcoretest.upload.UploadJobStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.*;

@RestController
@RequestMapping("/streaming/video")
@RequiredArgsConstructor
public class VideoStreamingController {

    private final VideoStreamingService videoStreamingService;
    private final UploadJobRegistry jobRegistry;
    private final FileMetadataRepo fileMetadataRepo;
    private final MinioService minioService;

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

    @GetMapping("/thumbnail/{uuid}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID uuid) throws Exception {
        FileMetadata meta = fileMetadataRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (meta.getThumbnailKey() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStream is = minioService.getThumbAsInputStream(meta);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }


    @GetMapping("/list")
    public List<FileMetadata> getVideoList() {
        return videoStreamingService.getVideoList();
    }

    @GetMapping("/list/my")
    public List<FileMetadata> getMyVideoList(Authentication authentication) {
        return videoStreamingService.getVideoList(videoStreamingService.extractUserId(authentication));
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadStartResponse> startUpload(
            @RequestParam("video") MultipartFile video,
            @RequestParam("videoName") String videoName,
            Authentication auth
    ) throws IOException {
        UUID jobId = videoStreamingService.enqueueUpload(video, videoName, auth);
        return ResponseEntity.ok(new UploadStartResponse(jobId));
    }

    @GetMapping("/upload/status/{jobId}")
    public ResponseEntity<UploadJobStatus> getUploadStatus(@PathVariable UUID jobId) {
        return jobRegistry.get(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record UploadStartResponse(UUID jobId) {}

    @PostMapping("/delete/{uuid}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID uuid) throws Exception {
        videoStreamingService.deleteVideo(uuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/upload/active")
    public ResponseEntity<List<UploadJobView>> getActiveUploadJobs(Authentication auth) {
        Long userId = videoStreamingService.extractUserId(auth); // такой же хелпер, как в LikeController
        List<UploadJobView> jobs = jobRegistry.getActiveJobsForUser(userId);
        return ResponseEntity.ok(jobs);
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
