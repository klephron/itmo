package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.integrations.s3.MinioService;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepo;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamRange;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoStreamingService {

    private final FileMetadataRepo fileMetadataRepo;
    private final MinioService minioService;

    public StreamChunk fetchChunk(UUID uuid, StreamRange range) throws Exception {
        FileMetadata meta = fileMetadataRepo.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + uuid));

        long fileSize = meta.getContentSize();
        long start = range.getRangeStart();
        if (start >= fileSize) {
            throw new IllegalArgumentException("Range start beyond file size");
        }
        long end = range.getRangeEnd(fileSize);
        long length = end - start + 1;

        try (InputStream is = minioService.getFileAsInputStream(meta, start, length);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            is.transferTo(baos);
            byte[] data = baos.toByteArray();
            return new StreamChunk(meta, data);
        }
    }

    public List<FileMetadata> getVideoList() {
        return fileMetadataRepo.findAll();
    }

    public FileMetadata saveVideo(MultipartFile video, String videoName) throws Exception {
        FileMetadata meta = new FileMetadata();
        meta.setName(videoName);
        meta.setUuid(UUID.randomUUID());

        // Получаем тип файла
        String contentType;
        String filename = Objects.requireNonNull(video.getOriginalFilename()).toLowerCase();
        if (filename.endsWith(".mp4")) {
            contentType = "video/mp4";
        } else if (filename.endsWith(".webm")) {
            contentType = "video/webm";
        } else if (filename.endsWith(".mov")) {
            contentType = "video/quicktime";
        } else if (filename.endsWith(".mkv")) {
            contentType = "video/x-matroska";
        } else {
            contentType = "application/octet-stream"; // fallback
        }

        meta.setContentType(contentType);
        meta.setContentSize(video.getSize());

        // Сохраняем метаданные
        meta = fileMetadataRepo.save(meta);

        // Загружаем в MinIO
        minioService.uploadFile(video, meta);

        return meta;
    }


    public void deleteVideo(UUID uuid) throws Exception {
        FileMetadata meta = fileMetadataRepo.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + uuid));

        minioService.deleteFile(meta.getUuid());
        fileMetadataRepo.delete(meta);
    }
}
