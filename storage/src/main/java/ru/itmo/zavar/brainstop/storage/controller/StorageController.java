package ru.itmo.zavar.brainstop.storage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.dto.storage.ObjectKeyResponse;
import ru.itmo.zavar.brainstop.storage.infra.MinioStorageService;

import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/internal/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Внутренний фасад над MinIO")
// MSPLIT: dedicated storage facade for all services
public class StorageController {

    private final VideoObjectStorage storage;

    @PostMapping(value = "/processed", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Загрузить processed-видео")
    public ResponseEntity<Void> uploadProcessed(@RequestParam("key") String key, HttpServletRequest request) throws Exception {
        long size = resolveSize(request);
        this.storage.uploadProcessed(key, request.getInputStream(), size);
        return ResponseEntity.accepted().build();
    }

    @GetMapping(value = "/processed", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Получить чанк processed-видео по смещению/длине")
    public ResponseEntity<Resource> openProcessed(@RequestParam("key") String key,
                                                  @RequestHeader("X-Range-Start") long start,
                                                  @RequestHeader("X-Range-Length") long length) throws Exception {
        InputStream is = this.storage.openProcessed(key, start, length);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(length);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes %d-%d/*".formatted(start, start + length - 1));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(is));
    }

    @GetMapping(value = "/processed/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Скачать processed-видео целиком")
    public ResponseEntity<Resource> downloadProcessed(@RequestParam("key") String key) throws Exception {
        InputStream is = this.storage.downloadProcessed(key);
        long size = -1;
        if (this.storage instanceof MinioStorageService minioStorageService) {
            try {
                size = minioStorageService.getProcessedObjectSize(key);
            } catch (Exception e) {
                log.warn("Failed to fetch object size for {}", key, e);
            }
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM);
        if (size >= 0) {
            builder.contentLength(size);
        }
        return builder.body(new InputStreamResource(is));
    }

    @DeleteMapping("/processed")
    @Operation(summary = "Удалить processed-видео")
    public ResponseEntity<Void> deleteProcessed(@RequestParam("key") String key) throws Exception {
        this.storage.deleteProcessed(key);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/thumbnails", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Загрузить thumbnail")
    public ResponseEntity<ObjectKeyResponse> uploadThumbnail(@RequestParam("key") String key, HttpServletRequest request) throws Exception {
        long size = resolveSize(request);
        String storedKey = this.storage.uploadThumbnail(key, request.getInputStream(), size);
        return ResponseEntity.ok(new ObjectKeyResponse(storedKey));
    }

    @GetMapping(value = "/thumbnails", produces = MediaType.IMAGE_JPEG_VALUE)
    @Operation(summary = "Получить thumbnail")
    public ResponseEntity<Resource> openThumbnail(@RequestParam("key") String key) throws Exception {
        InputStream is = this.storage.openThumbnail(key);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(is));
    }

    @PostMapping(value = "/avatars", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Загрузить аватар")
    public ResponseEntity<ObjectKeyResponse> uploadAvatar(@RequestParam("userId") Long userId,
                                                          @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
                                                          HttpServletRequest request) throws Exception {
        long size = resolveSize(request);
        String type = contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String key = this.storage.uploadAvatar(request.getInputStream(), size, type, userId);
        return ResponseEntity.ok(new ObjectKeyResponse(key));
    }

    @GetMapping(value = "/avatars", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Скачать аватар")
    public ResponseEntity<Resource> downloadAvatar(@RequestParam("key") String key) throws Exception {
        InputStream is = this.storage.downloadAvatar(key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(is));
    }

    private long resolveSize(HttpServletRequest request) {
        long size = request.getContentLengthLong();
        if (size >= 0) {
            return size;
        }
        String override = request.getHeader("X-Object-Size");
        if (override != null) {
            return Long.parseLong(override);
        }
        throw new IllegalArgumentException("Content-Length is required");
    }
}
