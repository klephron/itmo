package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.zavar.ticktokcoretest.dto.FeedVideoDto;
import ru.itmo.zavar.ticktokcoretest.dto.FileMetadataDto;
import ru.itmo.zavar.ticktokcoretest.dto.UploadJobViewDto;
import ru.itmo.zavar.ticktokcoretest.dto.response.UploadStartResponse;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.integrations.minio.MinioService;
import ru.itmo.zavar.ticktokcoretest.mapper.FileMetadataMapper;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;
import ru.itmo.zavar.ticktokcoretest.service.FeedService;
import ru.itmo.zavar.ticktokcoretest.service.VideoStreamingService;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamRange;
import ru.itmo.zavar.ticktokcoretest.transcode.UploadJobRegistry;
import ru.itmo.zavar.ticktokcoretest.transcode.UploadJobStatus;

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
    private final FileMetadataRepository fileMetadataRepository;
    private final MinioService minioService;
    private final FileMetadataMapper fileMetadataMapper;
    private final FeedService feedService;

    @Value("${streaming.chunk.default-size}")
    private long chunkDefaultSize;

    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> fetchChunk(@RequestHeader(value = RANGE, required = false) String range,
                                             @PathVariable UUID uuid) throws Exception {

        StreamRange parsedRange = StreamRange.parseHttpRangeString(range, this.chunkDefaultSize);
        StreamChunk streamChunk = this.videoStreamingService.fetchChunk(uuid, parsedRange);
        FileMetadataEntity meta = streamChunk.fileMetadataEntity();
        long fileSize = meta.getContentSize();

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE, meta.getContentType());
        headers.add(ACCEPT_RANGES, "bytes");
        headers.add(CONTENT_LENGTH, getContentLengthHeader(parsedRange, fileSize));
        headers.add(CONTENT_RANGE, getContentRangeHeader(parsedRange, fileSize));

        return new ResponseEntity<>(streamChunk.chunk(), headers, HttpStatus.PARTIAL_CONTENT);
    }

    @GetMapping("/thumbnail/{uuid}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID uuid) throws Exception {
        FileMetadataEntity meta = this.fileMetadataRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (meta.getThumbnailKey() == null) {
            return ResponseEntity.notFound().build();
        }
        InputStream is = this.minioService.getThumbnailAsInputStream(meta);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }


    @GetMapping("/list-all")
    public List<FileMetadataDto> getVideoList() {
        return this.fileMetadataMapper.toDto(this.videoStreamingService.getVideoList());
    }

    @GetMapping("/list")
    public ResponseEntity<Page<FeedVideoDto>> getVideoFeed(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        Long userId = authentication != null ? AuthService.extractUserId(authentication) : null;
        Page<FeedVideoDto> feed = this.feedService.getFeed(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/list/my")
    public List<FileMetadataDto> getMyVideoList(Authentication authentication) {
        return this.fileMetadataMapper.toDto(this.videoStreamingService.getVideoList(AuthService.extractUserId(authentication)));
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadStartResponse> startUpload(@RequestParam("video") MultipartFile video, @RequestParam("videoName") String videoName, Authentication auth
    ) throws IOException {
        UUID jobId = this.videoStreamingService.enqueueUpload(video, videoName, auth);
        return ResponseEntity.ok(new UploadStartResponse(jobId));
    }

    @GetMapping("/upload/status/{jobId}")
    public ResponseEntity<UploadJobStatus> getUploadStatus(@PathVariable UUID jobId) {
        return this.jobRegistry.get(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/delete/{uuid}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID uuid) throws Exception {
        this.videoStreamingService.deleteVideo(uuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/upload/active")
    public ResponseEntity<List<UploadJobViewDto>> getActiveUploadJobs(Authentication auth) {
        Long userId = AuthService.extractUserId(auth);
        List<UploadJobViewDto> jobs = this.jobRegistry.getActiveJobsForUser(userId);
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
