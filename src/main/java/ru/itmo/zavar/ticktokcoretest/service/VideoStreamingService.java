package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.integrations.minio.MinioService;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.streaming.StreamRange;
import ru.itmo.zavar.ticktokcoretest.transcode.JobStage;
import ru.itmo.zavar.ticktokcoretest.transcode.UploadJobRegistry;
import ru.itmo.zavar.ticktokcoretest.transcode.UploadJobStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStreamingService {

    private final FileMetadataRepository fileMetadataRepository;
    private final MinioService minioService;
    private final VideoTranscodingService transcodingService;
    private final UploadJobRegistry jobRegistry;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Value("${transcoder.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    public UUID enqueueUpload(MultipartFile video, String videoName, Authentication auth) throws IOException {
        UUID jobId = UUID.randomUUID();

        Long userId = AuthService.extractUserId(auth);
        String displayName = AuthService.extractDisplayName(auth);

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
        this.jobRegistry.put(jobId, status);

        Path tempInput = Files.createTempFile("upload-src-", ".tmp");
        video.transferTo(tempInput.toFile());

        this.executor.submit(() -> {
            try {
                processJob(jobId, tempInput, safeName, userId, displayName);
            } catch (Exception e) {
                log.error("Upload job {} failed", jobId, e);
                this.jobRegistry.update(jobId, st -> {
                    st.setStage(JobStage.ERROR);
                    st.setPercent(100);
                    st.setMessage("Ошибка: " + e.getMessage());
                });
            } finally {
                try {
                    Files.deleteIfExists(tempInput);
                } catch (Exception ignored) {
                }
            }
        });

        return jobId;
    }

    private void processJob(UUID jobId,
                            Path tempInput,
                            String videoName,
                            Long userId,
                            String displayName) throws Exception {

        this.jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.TRANSCODING);
            st.setPercent(0);
            st.setMessage("Подготовка к перекодированию…");
        });

        Path mp4Path = this.transcodingService.transcodeToMp4WithProgress(tempInput, jobId);
        long mp4Size = Files.size(mp4Path);

        FileMetadataEntity meta = new FileMetadataEntity();
        meta.setName(videoName);
        meta.setUuid(UUID.randomUUID());
        meta.setUploaderName(displayName);
        meta.setUploaderId(userId);
        meta.setContentType("video/mp4");
        meta.setContentSize(mp4Size);

        meta = this.fileMetadataRepository.save(meta);

        try {
            createAndUploadThumbnail(mp4Path, meta);
        } catch (Exception e) {
            log.error("Failed to create/upload thumbnail for {}", meta.getUuid(), e);
        }

        this.jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.UPLOADING_TO_MINIO);
            st.setPercent(100);
            st.setMessage("Загрузка в хранилище…");
        });

        try (InputStream is = Files.newInputStream(mp4Path)) {
            this.minioService.uploadVideoFile(is, mp4Size, meta);
        } finally {
            Files.deleteIfExists(mp4Path);
        }

        this.jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.DONE);
            st.setPercent(100);
            st.setMessage("Готово");
        });

        log.info("Upload job {} completed: uuid={}, name='{}', author='{}'",
                jobId, meta.getUuid(), meta.getName(), meta.getUploaderName());
    }

    private void createAndUploadThumbnail(Path videoPath, FileMetadataEntity meta) throws Exception {
        Path thumbPath = Files.createTempFile("thumb-", ".jpg");
        try {
            runFfmpegThumbnail(videoPath, thumbPath);

            if (!Files.exists(thumbPath) || Files.size(thumbPath) == 0) {
                throw new IllegalStateException("ffmpeg did not produce thumbnail file");
            }

            long thumbSize = Files.size(thumbPath);
            String thumbKey;
            try (InputStream is = Files.newInputStream(thumbPath)) {
                thumbKey = this.minioService.uploadThumbnail(is, thumbSize, meta);
            }

            meta.setThumbnailKey(thumbKey);
            this.fileMetadataRepository.save(meta);
            log.info("Thumbnail created for video {}", meta.getUuid());
        } finally {
            try {
                Files.deleteIfExists(thumbPath);
            } catch (Exception ignored) {
            }
        }
    }

    private void runFfmpegThumbnail(Path input, Path output) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                this.ffmpegPath,
                "-y",
                "-i", input.toString(),
                "-ss", "00:00:00.000",
                "-frames:v", "1",
                "-q:v", "2",
                output.toString()
        );
        pb.redirectErrorStream(true);
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

    public StreamChunk fetchChunk(UUID uuid, StreamRange range) throws Exception {
        FileMetadataEntity meta = this.fileMetadataRepository.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + uuid));

        long fileSize = meta.getContentSize();
        long start = range.getRangeStart();
        if (start >= fileSize) {
            throw new IllegalArgumentException("Range start beyond file size");
        }
        long end = range.getRangeEnd(fileSize);
        long length = end - start + 1;

        try (InputStream is = this.minioService.getVideoFileAsInputStream(meta, start, length);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            is.transferTo(baos);
            byte[] data = baos.toByteArray();
            return new StreamChunk(meta, data);
        }
    }

    public List<FileMetadataEntity> getVideoList(Long userId) {
        return this.fileMetadataRepository.findAllByUploaderId(userId);
    }

    public List<FileMetadataEntity> getVideoList() {
        return this.fileMetadataRepository.findAll();
    }

    public void deleteVideo(UUID uuid) throws Exception {
        FileMetadataEntity meta = this.fileMetadataRepository.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + uuid));

        this.minioService.deleteVideoFile(meta.getUuid());
        this.fileMetadataRepository.delete(meta);
    }

    public int regenerateAllThumbnails(boolean force) {
        List<FileMetadataEntity> all = this.fileMetadataRepository.findAll();
        int updated = 0;

        for (FileMetadataEntity meta : all) {
            if (!force && meta.getThumbnailKey() != null && !meta.getThumbnailKey().isBlank()) {
                continue;
            }

            Path tempVideo = null;
            try {
                tempVideo = Files.createTempFile("regen-src-", ".mp4");

                try (InputStream is = this.minioService.downloadVideoFile(meta)) {
                    Files.copy(is, tempVideo, StandardCopyOption.REPLACE_EXISTING);
                }
                createAndUploadThumbnail(tempVideo, meta);
                updated++;
            } catch (Exception e) {
                log.error("Failed to regenerate thumbnail for {}", meta.getUuid(), e);
            } finally {
                if (tempVideo != null) {
                    try {
                        Files.deleteIfExists(tempVideo);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        log.info("Thumbnail regeneration finished, updated = {}", updated);
        return updated;
    }
}
