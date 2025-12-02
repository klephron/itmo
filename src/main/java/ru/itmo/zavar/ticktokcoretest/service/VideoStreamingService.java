package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.integrations.s3.MinioService;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepo;
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
                              String uploaderName) throws java.io.IOException {

        UUID jobId = UUID.randomUUID();

        UploadJobStatus status = new UploadJobStatus(
                JobStage.UPLOADING,
                0,
                "Файл загружается",
                videoName == null || videoName.isBlank() ? video.getOriginalFilename() : videoName
        );
        jobRegistry.put(jobId, status);

        // сохраняем входной файл во временный
        Path tempInput = Files.createTempFile("upload-src-", ".tmp");
        video.transferTo(tempInput.toFile());

        String safeName = (videoName == null || videoName.isBlank())
                ? video.getOriginalFilename()
                : videoName;
        String safeAuthor = (uploaderName == null || uploaderName.isBlank())
                ? "Аноним"
                : uploaderName;

        executor.submit(() -> {
            try {
                processJob(jobId, tempInput, safeName, safeAuthor);
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
                            String uploaderName) throws Exception {

        // переходим в стадию перекодирования
        jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.TRANSCODING);
            st.setPercent(0);
            st.setMessage("Подготовка к перекодированию…");
        });

        // 1. ffmpeg с реальным прогрессом
        Path mp4Path = transcodingService.transcodeToMp4WithProgress(tempInput, jobId);
        long mp4Size = Files.size(mp4Path);

        // 2. сохраняем метаданные
        FileMetadata meta = new FileMetadata();
        meta.setName(videoName);
        meta.setUuid(UUID.randomUUID());
        meta.setUploaderName(uploaderName);
        meta.setContentType("video/mp4");
        meta.setContentSize(mp4Size);

        meta = fileMetadataRepo.save(meta);

        // 3. заливка в MinIO
        jobRegistry.update(jobId, st -> {
            st.setStage(JobStage.UPLOADING_TO_MINIO);
            st.setPercent(100); // можно сделать 90 и поднять до 100 в конце
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

    public void deleteVideo(UUID uuid) throws Exception {
        FileMetadata meta = fileMetadataRepo.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("File not found: " + uuid));

        minioService.deleteFile(meta.getUuid());
        fileMetadataRepo.delete(meta);
    }
}
