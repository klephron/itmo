package ru.itmo.zavar.brainstop.video.streaming.controller;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.UploadStartResponse;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamChunk;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamRange;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.api.transcoding.dto.UploadJobViewDto;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.service.FeedService;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.*;

@RestController
@RequestMapping("/api/streaming/video")
@RequiredArgsConstructor
@Tag(name = "Streaming", description = "Стриминг и загрузка видео")
// MSPLIT: HTTP API that will move to Streaming service
public class VideoStreamingController {

    private final VideoStreamingService videoStreamingService;
    private final TranscodingJobRegistry jobRegistry;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;
    private final FeedService feedService;
    private final VideoObjectStorage videoObjectStorage;

    @Value("${streaming.chunk.default-size}")
    private long chunkDefaultSize;

    @GetMapping("/{uuid}")
    @Operation(summary = "Отдать чанк видео по Range", description = "Возвращает 206 Partial Content для указанного видео и качества")
    public ResponseEntity<byte[]> fetchChunk(@RequestHeader(value = RANGE, required = false) String range,
                                             @PathVariable UUID uuid,
                                             @RequestParam(value = "quality", required = false) Integer quality) throws Exception {

        StreamRange parsedRange = StreamRange.parseHttpRangeString(range, this.chunkDefaultSize);
        StreamChunk streamChunk = this.videoStreamingService.fetchChunk(uuid, parsedRange, quality);
        FileMetadataEntity meta = streamChunk.fileMetadata();
        long fileSize = streamChunk.contentSize();

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE, streamChunk.contentType());
        headers.add(ACCEPT_RANGES, "bytes");
        headers.add(CONTENT_LENGTH, getContentLengthHeader(parsedRange, fileSize));
        headers.add(CONTENT_RANGE, getContentRangeHeader(parsedRange, fileSize));

        return new ResponseEntity<>(streamChunk.chunk(), headers, HttpStatus.PARTIAL_CONTENT);
    }

    @GetMapping("/thumbnail/{uuid}")
    @Operation(summary = "Получить превью видео")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID uuid) throws Exception {
        FileMetadataEntity meta = this.fileMetadataRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (meta.getThumbnailKey() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            InputStream is = this.videoObjectStorage.openThumbnail(meta.getThumbnailKey());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new InputStreamResource(is));
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/list-all")
    @Operation(summary = "Список всех видео (без пагинации)")
    public List<FileMetadataDto> getVideoList() {
        return this.fileMetadataMapper.toDto(this.videoStreamingService.getVideoList());
    }

    @GetMapping("/list")
    @Operation(summary = "Лента видео", description = "Пагинированная лента; авторизация не обязательна")
    public ResponseEntity<Page<FeedVideoDto>> getVideoFeed(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @AuthenticationPrincipal CurrentUser principal) {
        Long userId = principal != null ? principal.getId() : null;
        Page<FeedVideoDto> feed = this.feedService.getFeed(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/list/my")
    @Operation(summary = "Мои видео", description = "Требует авторизации")
    public List<FileMetadataDto> getMyVideoList(@AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return this.fileMetadataMapper.toDto(this.videoStreamingService.getVideoList(principal.getId()));
    }

    @PostMapping("/upload")
    @Operation(summary = "Начать загрузку видео", description = "Ставит задачу на загрузку/транскодирование")
    public ResponseEntity<UploadStartResponse> startUpload(@RequestParam("video") MultipartFile video,
                                                           @RequestParam("videoName") String videoName,
                                                           @RequestParam(value = "description", required = false) String description,
                                                           @AuthenticationPrincipal CurrentUser principal
    ) throws IOException {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        UUID jobId = this.videoStreamingService.enqueueUpload(video, videoName, description, principal.getId(), principal.getDisplayName());
        return ResponseEntity.ok(new UploadStartResponse(jobId));
    }

    @GetMapping("/upload/status/{jobId}")
    @Operation(summary = "Статус загрузки/транскода")
    public ResponseEntity<TranscodingJobStatus> getUploadStatus(@PathVariable UUID jobId) {
        return this.jobRegistry.get(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/delete/{uuid}")
    @Operation(summary = "Удалить видео", description = "Удаляет метаданные и файлы в хранилище")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID uuid) throws Exception {
        this.videoStreamingService.deleteVideo(uuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/upload/active")
    @Operation(summary = "Активные загрузки пользователя")
    public ResponseEntity<List<UploadJobViewDto>> getActiveUploadJobs(@AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Long userId = principal.getId();
        List<UploadJobViewDto> jobs = this.jobRegistry.getActiveJobsForUser(userId).entrySet().stream()
                .map(e -> new UploadJobViewDto(e.getKey(), e.getValue().getStage(), e.getValue().getPercent(), e.getValue().getMessage(), e.getValue().getFileName()))
                .toList();
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
