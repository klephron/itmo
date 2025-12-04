package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStreamingService {

    private final FileMetadataRepo fileMetadataRepo;
    private final MinioService minioService;
    private final VideoTranscodingService transcodingService;
    private final UploadJobRegistry jobRegistry;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Value("${transcoder.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    public UUID enqueueUpload(MultipartFile video,
                              String videoName,
                              Authentication auth) throws java.io.IOException {

        UUID jobId = UUID.randomUUID();

        Long userId = extractUserId(auth);
        String displayName = extractDisplayName(auth);

        String safeName = (videoName == null || videoName.isBlank())
                ? video.getOriginalFilename()
                : videoName;

        UploadJobStatus status = new UploadJobStatus(
                JobStage.UPLOADING,
                0,
                "Файл загружается",
                safeName,
                userId
        );
        jobRegistry.put(jobId, status);

        Path tempInput = Files.createTempFile("upload-src-", ".tmp");
        video.transferTo(tempInput.toFile());

        executor.submit(() -> {
            try {
                processJob(jobId, tempInput, safeName, userId, displayName);
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
        meta.setUploaderId(userId);
        meta.setContentType("video/mp4");
        meta.setContentSize(mp4Size);

        meta = fileMetadataRepo.save(meta);

        // ---------- превью ----------
        try {
            createAndUploadThumbnail(mp4Path, meta);
        } catch (Exception e) {
            // превью не критично – не роняем джоб
            log.error("Failed to create/upload thumbnail for {}", meta.getUuid(), e);
        }
        // ---------- конец превью ----------

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

    /**
     * Общий хелпер: берёт видео, выдёргивает первый кадр через ffmpeg,
     * загружает в MinIO и сохраняет thumbnailKey в meta.
     */
    private void createAndUploadThumbnail(Path videoPath, FileMetadata meta) throws Exception {
        Path thumbPath = Files.createTempFile("thumb-", ".jpg");
        try {
            runFfmpegThumbnail(videoPath, thumbPath);

            if (!Files.exists(thumbPath) || Files.size(thumbPath) == 0) {
                throw new IllegalStateException("ffmpeg did not produce thumbnail file");
            }

            long thumbSize = Files.size(thumbPath);
            String thumbKey;
            try (InputStream is = Files.newInputStream(thumbPath)) {
                thumbKey = minioService.uploadThumbnail(is, thumbSize, meta);
            }

            meta.setThumbnailKey(thumbKey);
            fileMetadataRepo.save(meta);
            log.info("Thumbnail created for video {}", meta.getUuid());
        } finally {
            try {
                Files.deleteIfExists(thumbPath);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Запускает ffmpeg, глушит вывод и ждёт с таймаутом.
     */
    private void runFfmpegThumbnail(Path input, Path output) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", input.toString(),
                "-ss", "00:00:00.000",
                "-frames:v", "1",
                "-q:v", "2",
                output.toString()
        );
        pb.redirectErrorStream(true);
        // чтобы stdout не забивал буфер
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process p = pb.start();

        boolean finished = p.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IllegalStateException("ffmpeg timeout for " + input);
        }

        int code = p.exitValue();
        if (code != 0) {
            throw new IllegalStateException("ffmpeg exit code " + code + " for " + input);
        }
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

    /**
     * Массовая регенерация превью.
     * @param force true — пересоздавать даже если thumbnail уже есть.
     * @return сколько превью обновили.
     */
    public int regenerateAllThumbnails(boolean force) {
        List<FileMetadata> all = fileMetadataRepo.findAll();
        int updated = 0;

        for (FileMetadata meta : all) {
            if (!force && meta.getThumbnailKey() != null && !meta.getThumbnailKey().isBlank()) {
                continue;
            }

            Path tempVideo = null;
            try {
                tempVideo = Files.createTempFile("regen-src-", ".mp4");

                // 1. скачиваем видео из MinIO во временный файл
                try (InputStream is = minioService.downloadVideo(meta)) {
                    Files.copy(is, tempVideo, StandardCopyOption.REPLACE_EXISTING);
                }

                // 2. создаём и заливаем превью
                createAndUploadThumbnail(tempVideo, meta);
                updated++;
            } catch (Exception e) {
                log.error("Failed to regenerate thumbnail for {}", meta.getUuid(), e);
            } finally {
                if (tempVideo != null) {
                    try { Files.deleteIfExists(tempVideo); } catch (Exception ignored) {}
                }
            }
        }

        log.info("Thumbnail regeneration finished, updated = {}", updated);
        return updated;
    }
}
