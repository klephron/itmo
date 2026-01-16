package ru.itmo.zavar.brainstop.video.streaming.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodedRenditionMetadata;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingCommand;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingResult;
import ru.itmo.zavar.brainstop.search.client.SearchEmbeddingClient;
import ru.itmo.zavar.brainstop.transcoding.config.TranscodingKafkaConfig;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmResult;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmService;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.model.VideoRenditionEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamChunk;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamRange;
import ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity;
import ru.itmo.zavar.brainstop.video.transcoding.repo.UploadJobRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
// MSPLIT: HTTP upload/streaming boundary for future Video/Streaming service
@RequiredArgsConstructor
public class VideoStreamingService {

    private static final int EMBEDDING_DIM = 1024;
    private static final List<Integer> DEFAULT_TARGET_HEIGHTS = List.of(144, 240, 360, 480, 720, 1080);

    private final FileMetadataRepository fileMetadataRepository;
    private final VideoObjectStorage videoObjectStorage;
    private final TranscodingJobRegistry jobRegistry;
    private final UploadJobRepository uploadJobRepository;
    private final SearchEmbeddingClient embeddingClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final VideoLlmService videoLlmService;

    @Value("${transcoder.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    /**
    * Принимает файл, кладёт его в хранилище и отправляет задачу в очередь транскодинга.
    */
    public UUID enqueueUpload(MultipartFile video, String videoName, String description, Long userId, String displayName) throws IOException {
        String safeName = (videoName == null || videoName.isBlank())
                ? video.getOriginalFilename()
                : videoName;

        // Идемпотентность: если уже есть активное задание с таким именем у пользователя — вернём его id
        Optional<UploadJobEntity> existing = uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(
                userId,
                safeName,
                List.of(TranscodingJobStage.UPLOADING, TranscodingJobStage.TRANSCODING, TranscodingJobStage.UPLOADING_TO_MINIO)
        );
        if (existing.isPresent()) {
            UUID existingId = existing.get().getId();
            log.info("Reusing active upload {} for user {} and name {}", existingId, userId, safeName);
            return existingId;
        }

        UUID jobId = UUID.randomUUID();
        String storageKey = "raw/" + jobId + ".mp4";

        UploadJobEntity entity = new UploadJobEntity();
        entity.setId(jobId);
        entity.setStorageKey(storageKey);
        entity.setVideoName(safeName);
        entity.setDescription(description);
        entity.setUserId(userId);
        entity.setDisplayName(displayName);
        entity.setStage(TranscodingJobStage.UPLOADING);
        entity.setPercent(0);
        entity.setMessage("Файл загружается");
        uploadJobRepository.save(entity);

        jobRegistry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.UPLOADING,
                0,
                "Файл загружается",
                safeName,
                userId
        ));

        // Загружаем сразу (без фонового executor), чтобы избежать ошибок multipart
        try (InputStream is = video.getInputStream()) {
            videoObjectStorage.uploadProcessed(storageKey, is, video.getSize());
        } catch (Exception e) {
            log.error("Failed to upload raw video {} to storage", jobId, e);
            jobRegistry.update(jobId, st -> {
                st.setStage(TranscodingJobStage.ERROR);
                st.setPercent(100);
                st.setMessage("Ошибка загрузки: " + e.getMessage());
            });
            throw new IOException("Failed to upload raw video to storage", e);
        }

        jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.TRANSCODING);
            st.setPercent(0);
            st.setMessage("В очереди на транскодинг…");
        });

        TranscodingCommand command = new TranscodingCommand(
                jobId,
                storageKey,
                safeName,
                description,
                userId,
                displayName,
                DEFAULT_TARGET_HEIGHTS
        );
        // Use jobId as key so Kafka spreads messages across partitions -> multiple transcoders share the load
        kafkaTemplate.send(TranscodingKafkaConfig.TOPIC_COMMAND, command.jobId().toString(), command);
        log.info("Upload job {} placed to queue, key={}", jobId, storageKey);

        return jobId;
    }

    /**
     * При старте инстанса повторно ставим в очередь "подвисшие" задачи (TRANSCODING/UPLOADING_TO_MINIO),
     * чтобы их подхватил живой транскодер.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void requeueStuckJobs() {
        List<UploadJobEntity> stuck = uploadJobRepository.findByStageIn(
                List.of(TranscodingJobStage.TRANSCODING, TranscodingJobStage.UPLOADING_TO_MINIO));
        if (stuck.isEmpty()) {
            return;
        }
        log.info("Found {} stuck upload jobs, requeueing…", stuck.size());
        for (UploadJobEntity job : stuck) {
            try {
                jobRegistry.put(job.getId(), new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, 0, "В очереди (recovery)…", job.getVideoName(), job.getUserId()
                ));
                TranscodingCommand command = new TranscodingCommand(
                        job.getId(),
                        job.getStorageKey(),
                        job.getVideoName(),
                        job.getDescription(),
                        job.getUserId(),
                        job.getDisplayName(),
                        DEFAULT_TARGET_HEIGHTS
                );
                kafkaTemplate.send(TranscodingKafkaConfig.TOPIC_COMMAND, command.jobId().toString(), command);
                log.info("Requeued stuck job {} ({})", job.getId(), job.getVideoName());
            } catch (Exception e) {
                log.error("Failed to requeue job {}", job.getId(), e);
            }
        }
    }

    /**
     * Обрабатывает завершённый результат транскодинга: создаёт метаданные видео и закрывает задачу.
     */
    public void handleTranscodingResult(TranscodingResult result) {
        if (result == null || result.jobId() == null) {
            return;
        }
        UploadJobEntity job = uploadJobRepository.findById(result.jobId()).orElse(null);
        if (job == null) {
            log.warn("Received result for unknown job {}", result.jobId());
            return;
        }

        try {
            log.info("Handling transcoding result for job {}", result.jobId());
            jobRegistry.update(result.jobId(), st -> {
                st.setStage(TranscodingJobStage.DONE);
                st.setPercent(100);
                st.setMessage("Готово");
            });

            FileMetadataEntity meta = new FileMetadataEntity();
            meta.setUuid(UUID.randomUUID());
            meta.setName(job.getVideoName());
            meta.setDescription(job.getDescription());
            meta.setUploaderId(job.getUserId());
            meta.setUploaderName(job.getDisplayName());
            meta.setContentType("video/mp4");

            List<TranscodedRenditionMetadata> renditions = Optional.ofNullable(result.renditions()).orElse(List.of());
            TranscodedRenditionMetadata primary = renditions.stream()
                    .max(Comparator.comparingInt(TranscodedRenditionMetadata::height))
                    .orElseThrow(() -> new IllegalStateException("Нет сконвертированных версий"));

            meta.setDefaultHeight(result.defaultHeight() != null ? result.defaultHeight() : primary.height());
            meta.setContentSize(primary.size());
            meta.setThumbnailKey(result.thumbnailKey());

            float[] titleVec = fetchEmbedding(meta.getName());
            float[] descVec = fetchEmbedding(meta.getDescription());
            float[] combined = fetchEmbedding((meta.getName() == null ? "" : meta.getName()) + " " + (meta.getDescription() == null ? "" : meta.getDescription()));
            meta.setTitleEmbedding(titleVec);
            meta.setDescriptionEmbedding(descVec);
            meta.setCombinedEmbedding(combined);

            try {
                VideoLlmResult llmResult = videoLlmService.enrich(meta.getName(), meta.getDescription());
                if (llmResult != null) {
                    String tagsText = llmResult.tagsText();
                    if (!tagsText.isBlank()) {
                        meta.setLlmTags(tagsText);
                    }
                    meta.setLlmFlagged(llmResult.flagged());
                    String flagsText = llmResult.categoriesText();
                    if (!flagsText.isBlank()) {
                        meta.setLlmFlags(flagsText);
                    }
                }
            } catch (Exception e) {
                log.warn("LLM enrichment failed for {}", meta.getUuid(), e);
            }

            List<VideoRenditionEntity> renditionEntities = renditions.stream()
                    .sorted(Comparator.comparingInt(TranscodedRenditionMetadata::height))
                    .map(r -> {
                        VideoRenditionEntity ent = new VideoRenditionEntity();
                        ent.setFile(meta);
                        ent.setHeight(r.height());
                        ent.setContentType(r.contentType());
                        ent.setContentSize(r.size());
                        ent.setStorageKey(r.storageKey());
                        return ent;
                    })
                    .toList();

            meta.getRenditions().clear();
            meta.getRenditions().addAll(renditionEntities);
            fileMetadataRepository.save(meta);

            // Чистим исходник
            if (job.getStorageKey() != null) {
                try {
                    videoObjectStorage.deleteProcessed(job.getStorageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete raw object {}: {}", job.getStorageKey(), e.getMessage());
                }
            }

            jobRegistry.update(result.jobId(), st -> {
                st.setStage(TranscodingJobStage.DONE);
                st.setPercent(100);
                st.setMessage("Готово");
            });
            log.info("Upload job {} completed, video uuid={} renditions={}", result.jobId(), meta.getUuid(), renditions.size());
        } catch (Exception e) {
            log.error("Failed to handle transcoding result for job {}", result.jobId(), e);
            jobRegistry.update(result.jobId(), st -> {
                st.setStage(TranscodingJobStage.ERROR);
                st.setPercent(100);
                st.setMessage("Ошибка: " + e.getMessage());
            });
        }
    }

    private float[] fetchEmbedding(String text) {
        try {
            var resp = embeddingClient.embed(text == null ? "" : text);
            if (resp != null && resp.vector() != null) {
                List<Float> v = resp.vector();
                float[] arr = new float[v.size()];
                for (int i = 0; i < v.size(); i++) {
                    arr[i] = v.get(i) != null ? v.get(i) : 0f;
                }
                return arr;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch embedding, returning zeros", e);
        }
        return new float[0];
    }

    private float[] normalizeEmbedding(float[] vec) {
        float[] safe = vec == null ? new float[EMBEDDING_DIM] : Arrays.copyOf(vec, EMBEDDING_DIM);
        double norm = 0.0;
        for (float v : safe) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return safe;
        }
        float[] out = new float[safe.length];
        for (int i = 0; i < safe.length; i++) {
            out[i] = (float) (safe[i] / norm);
        }
        return out;
    }

    // --- Streaming / read path ---

    public StreamChunk fetchChunk(UUID uuid, StreamRange range, Integer quality) throws Exception {
        FileMetadataEntity meta = this.fileMetadataRepository.findWithRenditionsByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uuid));

        VideoRenditionEntity rendition = pickRendition(meta, quality);

        long fileSize = rendition != null ? rendition.getContentSize() : meta.getContentSize();
        long start = range.getRangeStart();
        if (start >= fileSize) {
            throw new IllegalArgumentException("Range start beyond file size");
        }
        long end = range.getRangeEnd(fileSize);
        long length = end - start + 1;

        String storageKey = rendition != null ? rendition.getStorageKey() : meta.getUuid().toString();

        try (InputStream is = this.videoObjectStorage.openProcessed(storageKey, start, length);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            is.transferTo(baos);
            byte[] data = baos.toByteArray();
            return new StreamChunk(meta, rendition, data);
        }
    }

    public List<FileMetadataEntity> getVideoList(Long userId) {
        return this.fileMetadataRepository.findAllByUploaderId(userId);
    }

    public List<FileMetadataEntity> getVideoList() {
        return this.fileMetadataRepository.findAll();
    }

    public void deleteVideo(UUID uuid) throws Exception {
        FileMetadataEntity meta = this.fileMetadataRepository.findWithRenditionsByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uuid));

        if (meta.getRenditions() != null && !meta.getRenditions().isEmpty()) {
            for (VideoRenditionEntity rendition : meta.getRenditions()) {
                this.videoObjectStorage.deleteProcessed(rendition.getStorageKey());
            }
        } else {
            this.videoObjectStorage.deleteProcessed(meta.getUuid().toString());
        }
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

                String storageKey = resolvePreferredStorageKey(meta);

                try (InputStream is = this.videoObjectStorage.downloadProcessed(storageKey)) {
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

    public int rebuildEmbeddings() {
        List<FileMetadataEntity> all = this.fileMetadataRepository.findAll();
        int total = all.size();
        int processed = 0;
        int logEvery = Math.max(1, total / 20);
        log.info("Embedding rebuild started, total={}", total);
        int updated = 0;
        for (FileMetadataEntity meta : all) {
            try {
                float[] title = normalizeEmbedding(fetchEmbedding(meta.getName()));
                float[] desc = normalizeEmbedding(fetchEmbedding(meta.getDescription()));
                String combinedText = (meta.getName() == null ? "" : meta.getName()) + " " + (meta.getDescription() == null ? "" : meta.getDescription());
                float[] combined = normalizeEmbedding(fetchEmbedding(combinedText));
                meta.setTitleEmbedding(title);
                meta.setDescriptionEmbedding(desc);
                meta.setCombinedEmbedding(combined);
                this.fileMetadataRepository.save(meta);
                updated++;
            } catch (Exception e) {
                log.warn("Failed to rebuild embeddings for {}", meta.getUuid(), e);
            }
            processed++;
            if (processed % logEvery == 0 || processed == total) {
                int percent = total == 0 ? 100 : (int) Math.round((processed * 100.0) / total);
                log.info("Embedding rebuild progress: {}/{} ({}%)", processed, total, percent);
            }
        }
        log.info("Embedding rebuild finished, updated = {}", updated);
        return updated;
    }

    private VideoRenditionEntity pickRendition(FileMetadataEntity meta, Integer requestedHeight) {
        List<VideoRenditionEntity> renditions = Optional.ofNullable(meta.getRenditions())
                .orElse(List.of());
        if (renditions.isEmpty()) {
            return null;
        }

        List<VideoRenditionEntity> sorted = renditions.stream()
                .sorted(Comparator.comparingInt(VideoRenditionEntity::getHeight))
                .toList();

        if (requestedHeight == null) {
            return pickDefaultRendition(sorted, meta.getDefaultHeight());
        }

        Optional<VideoRenditionEntity> exact = sorted.stream()
                .filter(r -> Objects.equals(r.getHeight(), requestedHeight))
                .findFirst();
        if (exact.isPresent()) {
            return exact.get();
        }

        Optional<VideoRenditionEntity> lowerOrEqual = sorted.stream()
                .filter(r -> r.getHeight() <= requestedHeight)
                .max(Comparator.comparingInt(VideoRenditionEntity::getHeight));

        return lowerOrEqual.orElseGet(() -> pickDefaultRendition(sorted, meta.getDefaultHeight()));
    }

    private VideoRenditionEntity pickDefaultRendition(List<VideoRenditionEntity> sorted, Integer defaultHeight) {
        if (defaultHeight != null) {
            for (VideoRenditionEntity rendition : sorted) {
                if (Objects.equals(rendition.getHeight(), defaultHeight)) {
                    return rendition;
                }
            }
        }
        return sorted.get(sorted.size() - 1);
    }

    private String resolvePreferredStorageKey(FileMetadataEntity meta) {
        VideoRenditionEntity rendition = pickRendition(meta, meta.getDefaultHeight());
        if (rendition != null) {
            return rendition.getStorageKey();
        }
        return meta.getUuid().toString();
    }

    // --- Thumbnail helpers (для регенерации) ---
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
                thumbKey = this.videoObjectStorage.uploadThumbnail("thumbnails/" + meta.getUuid() + ".jpg", is, thumbSize);
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
}
