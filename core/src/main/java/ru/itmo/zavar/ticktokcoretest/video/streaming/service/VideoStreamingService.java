package ru.itmo.zavar.ticktokcoretest.video.streaming.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.ticktokcoretest.search.EmbeddingService;
import ru.itmo.zavar.ticktokcoretest.video.streaming.model.StreamChunk;
import ru.itmo.zavar.ticktokcoretest.video.streaming.model.StreamRange;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodedRendition;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.VideoTranscoder;
import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.video.model.VideoRenditionEntity;
import ru.itmo.zavar.ticktokcoretest.video.repo.FileMetadataRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
// MSPLIT: HTTP upload/streaming boundary for future Video/Streaming service
@RequiredArgsConstructor
public class VideoStreamingService {

    private static final List<Integer> DEFAULT_TARGET_HEIGHTS = List.of(144, 240, 360, 480, 720, 1080);

    private final FileMetadataRepository fileMetadataRepository;
    private final VideoObjectStorage videoObjectStorage;
    private final VideoTranscoder transcodingService;
    private final TranscodingJobRegistry jobRegistry;
    private final EmbeddingService embeddingService;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Value("${transcoder.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    public UUID enqueueUpload(MultipartFile video, String videoName, String description, Long userId, String displayName) throws IOException {
        UUID jobId = UUID.randomUUID();

        String safeName = (videoName == null || videoName.isBlank())
                ? video.getOriginalFilename()
                : videoName;

        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.UPLOADING,
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
                processJob(jobId, tempInput, safeName, description, userId, displayName);
            } catch (Exception e) {
                log.error("Upload job {} failed", jobId, e);
                this.jobRegistry.update(jobId, st -> {
                    st.setStage(TranscodingJobStage.ERROR);
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
                            String description,
                            Long userId,
                            String displayName) throws Exception {

        this.jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.TRANSCODING);
            st.setPercent(0);
            st.setMessage("Подготовка к перекодированию…");
        });

        List<TranscodedRendition> renditions = this.transcodingService
                .transcodeToMp4WithProgress(tempInput, jobId, DEFAULT_TARGET_HEIGHTS);

        Map<Integer, TranscodedRendition> byHeight = new HashMap<>();
        for (TranscodedRendition r : renditions) {
            byHeight.put(r.height(), r);
        }

        TranscodedRendition primary = renditions.stream()
                .max(Comparator.comparingInt(TranscodedRendition::height))
                .orElseThrow(() -> new IllegalStateException("No renditions were produced"));

        FileMetadataEntity meta = new FileMetadataEntity();
        meta.setName(videoName);
        meta.setUuid(UUID.randomUUID());
        meta.setUploaderName(displayName);
        meta.setUploaderId(userId);
        meta.setContentType("video/mp4");
        meta.setContentSize(primary.size());
        meta.setDefaultHeight(primary.height());
        meta.setDescription(description);

        float[] titleVec = this.embeddingService.embed(videoName);
        float[] descVec = this.embeddingService.embed(description);
        float[] combined = this.embeddingService.embed(
                (videoName == null ? "" : videoName) + " " + (description == null ? "" : description)
        );

        meta.setTitleEmbedding(titleVec);
        meta.setDescriptionEmbedding(descVec);
        meta.setCombinedEmbedding(combined);

        final FileMetadataEntity metaRef = meta;
        List<VideoRenditionEntity> renditionEntities = renditions.stream()
                .sorted(Comparator.comparingInt(TranscodedRendition::height))
                .map(r -> {
                    VideoRenditionEntity ent = new VideoRenditionEntity();
                    ent.setFile(metaRef);
                    ent.setHeight(r.height());
                    ent.setContentType("video/mp4");
                    ent.setContentSize(r.size());
                    ent.setStorageKey(buildRenditionKey(metaRef, r.height()));
                    return ent;
                })
                .toList();

        meta.getRenditions().clear();
        meta.getRenditions().addAll(renditionEntities);
        meta = this.fileMetadataRepository.save(meta);

        try {
            createAndUploadThumbnail(primary.path(), meta);
        } catch (Exception e) {
            log.error("Failed to create/upload thumbnail for {}", meta.getUuid(), e);
        }

        this.jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.UPLOADING_TO_MINIO);
            st.setPercent(100);
            st.setMessage("Загрузка в хранилище…");
        });

        for (int i = 0; i < renditionEntities.size(); i++) {
            VideoRenditionEntity rendition = renditionEntities.get(i);
            TranscodedRendition produced = byHeight.get(rendition.getHeight());
            if (produced == null) {
                continue;
            }
            try (InputStream is = Files.newInputStream(produced.path())) {
                this.videoObjectStorage.uploadProcessed(rendition.getStorageKey(), is, produced.size());
            }
        }

        for (TranscodedRendition r : renditions) {
            try {
                Files.deleteIfExists(r.path());
            } catch (Exception ignored) {
            }
        }

        this.jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.DONE);
            st.setPercent(100);
            st.setMessage("Готово");
        });

        log.info("Upload job {} completed: uuid={}, name='{}', author='{}', qualities={}",
                jobId, meta.getUuid(), meta.getName(), meta.getUploaderName(), byHeight.keySet());
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

    private String buildRenditionKey(FileMetadataEntity meta, int height) {
        return "processed/" + meta.getUuid() + "/" + height + "p.mp4";
    }

    private String resolvePreferredStorageKey(FileMetadataEntity meta) {
        VideoRenditionEntity rendition = pickRendition(meta, meta.getDefaultHeight());
        if (rendition != null) {
            return rendition.getStorageKey();
        }
        return meta.getUuid().toString();
    }
}
