package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.integrations.s3.MinioService;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepo;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamRange;
import ru.itmo.zavar.ticktokcoretest.upload.JobStage;
import ru.itmo.zavar.ticktokcoretest.upload.UploadJobRegistry;
import ru.itmo.zavar.ticktokcoretest.upload.UploadJobStatus;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStreamingService {

    private final FileMetadataRepo fileMetadataRepo;
    private final MinioService minioService;
    private final VideoTranscodingService transcodingService;
    private final UploadJobRegistry jobRegistry;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public UUID enqueueUpload(MultipartFile video,
                              String videoName,
                              Authentication auth) throws java.io.IOException {

        UUID jobId = UUID.randomUUID();

        Long userId = extractUserId(auth);              // 🔹 уже есть метод ниже
        String displayName = extractDisplayName(auth);  // 🔹 тоже есть

        String safeName = (videoName == null || videoName.isBlank())
                ? video.getOriginalFilename()
                : videoName;

        UploadJobStatus status = new UploadJobStatus(
                JobStage.UPLOADING,
                0,
                "Файл загружается",
                safeName,
                userId                               // 🔹 записали владельца
        );
        jobRegistry.put(jobId, status);

        Path tempInput = Files.createTempFile("upload-src-", ".tmp");
        video.transferTo(tempInput.toFile());

        executor.submit(() -> {
            try {
                processJob(jobId, tempInput, safeName, userId, displayName);  // 🔹 auth больше не таскаем
            } catch (Exception e) {
                log.error("Upload job {} failed", jobId, e);
                jobRegistry.update(jobId, st -> {
                    st.setStage(JobStage.ERROR);
                    st.setPercent(100);
                    st.setMessage("Ошибка: " + e.getMessage());
                });
            } finally {
                try {
                    Files.deleteIfExists(tempInput);
                } catch (Exception ignored) {}
            }
        });

        return jobId;
    }


    private void processJob(UUID jobId,
                            Path tempInput,
                            String videoName,
                            Long userId,
                            String displayName) throws Exception {

        // userId и displayName уже есть, auth не нужен

        jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.TRANSCODING);
            st.setPercent(0);
            st.setMessage("Подготовка к перекодированию…");
        });

        Path mp4Path = transcodingService.transcodeToMp4WithProgress(tempInput, jobId);
        long mp4Size = Files.size(mp4Path);

        FileMetadata meta = new FileMetadata();
        meta.setName(videoName);
        meta.setUuid(UUID.randomUUID());
        meta.setUploaderName(displayName);
        meta.setUploaderId(userId);           // 🔹 владелец файла
        meta.setContentType("video/mp4");
        meta.setContentSize(mp4Size);

        meta = fileMetadataRepo.save(meta);

        jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.UPLOADING_TO_MINIO);
            st.setPercent(100);
            st.setMessage("Загрузка в хранилище…");
        });

        try (InputStream is = Files.newInputStream(mp4Path)) {
            minioService.uploadFile(is, mp4Size, meta);
        } finally {
            Files.deleteIfExists(mp4Path);
        }

        jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.DONE);
            st.setPercent(100);
            st.setMessage("Готово");
        });

        log.info("Upload job {} completed: uuid={}, name='{}', author='{}'",
                jobId, meta.getUuid(), meta.getName(), meta.getUploaderName());
    }


    private String extractDisplayName(Authentication auth) {
        if (auth == null) return "Аноним";
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getDisplayName();
        }
        return "Аноним";
    }

    public Long extractUserId(Authentication authentication) {
        if (authentication == null) throw new RuntimeException("Unauthenticated");
        Object details = authentication.getDetails();
        if (details instanceof Map<?,?> map && map.get("id") != null) {
            return Long.valueOf(map.get("id").toString());
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getId();
        }
        throw new RuntimeException("Cannot extract user id");
    }


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

    public List<FileMetadata> getVideoList(Long userId) {
        return fileMetadataRepo.findAllByUploaderId(userId);
    }

    public List<FileMetadata> getVideoList() {
        return fileMetadataRepo.findAll();
    }

    public void deleteVideo(UUID uuid) throws Exception {
        FileMetadata meta = fileMetadataRepo.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + uuid));

        minioService.deleteFile(meta.getUuid());
        fileMetadataRepo.delete(meta);
    }
}
