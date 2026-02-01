package ru.itmo.zavar.brainstop.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchEmbeddingClient;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmResult;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmService;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.model.VideoRenditionEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamChunk;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamRange;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;
import ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity;
import ru.itmo.zavar.brainstop.video.transcoding.repo.UploadJobRepository;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.3", "FR-3.1"})
@Layer(TestLayer.BE_UNIT)
class VideoStreamingServiceComplexTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private VideoObjectStorage videoObjectStorage;
    @Mock
    private TranscodingJobRegistry jobRegistry;
    @Mock
    private UploadJobRepository uploadJobRepository;
    @Mock
    private SearchEmbeddingClient embeddingClient;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private VideoLlmService videoLlmService;
    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private VideoStreamingService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ffmpegPath", "ffmpeg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-01: handleTranscodingResult создает metadata с renditions")
    void handleTranscodingResult_createsMetadataWithRenditions() {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test Video", 1L, "User");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(
                        new TranscodedRenditionMetadata(360, "key360", 1000L, "video/mp4"),
                        new TranscodedRenditionMetadata(720, "key720", 2000L, "video/mp4"),
                        new TranscodedRenditionMetadata(1080, "key1080", 3000L, "video/mp4")
                ),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f, 0.2f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        ArgumentCaptor<FileMetadataEntity> captor = ArgumentCaptor.forClass(FileMetadataEntity.class);
        verify(fileMetadataRepository).save(captor.capture());

        FileMetadataEntity saved = captor.getValue();
        assertThat(saved.getRenditions()).hasSize(3);
        assertThat(saved.getDefaultHeight()).isEqualTo(720);
        assertThat(saved.getThumbnailKey()).isEqualTo("thumb.jpg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-02: handleTranscodingResult сортирует renditions")
    void handleTranscodingResult_sortsRenditions() {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test", 1L, "User");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(
                        new TranscodedRenditionMetadata(1080, "key1080", 3000L, "video/mp4"),
                        new TranscodedRenditionMetadata(360, "key360", 1000L, "video/mp4"),
                        new TranscodedRenditionMetadata(720, "key720", 2000L, "video/mp4")
                ),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        ArgumentCaptor<FileMetadataEntity> captor = ArgumentCaptor.forClass(FileMetadataEntity.class);
        verify(fileMetadataRepository).save(captor.capture());

        List<VideoRenditionEntity> renditions = captor.getValue().getRenditions();
        assertThat(renditions.get(0).getHeight()).isEqualTo(360);
        assertThat(renditions.get(1).getHeight()).isEqualTo(720);
        assertThat(renditions.get(2).getHeight()).isEqualTo(1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-03: handleTranscodingResult создает embeddings")
    void handleTranscodingResult_createsEmbeddings() {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test Video", 1L, "User");
        job.setDescription("Description");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f, 0.2f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        verify(embeddingClient, times(3)).embed(anyString());
        ArgumentCaptor<FileMetadataEntity> captor = ArgumentCaptor.forClass(FileMetadataEntity.class);
        verify(fileMetadataRepository).save(captor.capture());

        FileMetadataEntity saved = captor.getValue();
        assertThat(saved.getTitleEmbedding()).isNotNull();
        assertThat(saved.getDescriptionEmbedding()).isNotNull();
        assertThat(saved.getCombinedEmbedding()).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-05: handleTranscodingResult удаляет raw file")
    void handleTranscodingResult_deletesRawFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test", 1L, "User");
        job.setStorageKey("raw/test.mp4");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        verify(videoObjectStorage).deleteProcessed("raw/test.mp4");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-06: handleTranscodingResult обновляет job status")
    void handleTranscodingResult_updatesJobStatus() {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test", 1L, "User");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        verify(jobRegistry, times(2)).update(eq(jobId), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-08: fetchChunk выбирает rendition по quality")
    void fetchChunk_picksRenditionByQuality() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntityWithRenditions(uuid);

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));
        when(videoObjectStorage.openProcessed(anyString(), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-99", 1000);
        StreamChunk chunk = service.fetchChunk(uuid, range, 720);

        assertThat(chunk.rendition()).isNotNull();
        assertThat(chunk.rendition().getHeight()).isEqualTo(720);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-09: fetchChunk выбирает ближайшее качество")
    void fetchChunk_picksClosestQuality() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntityWithRenditions(uuid);

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));
        when(videoObjectStorage.openProcessed(anyString(), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-99", 1000);
        StreamChunk chunk = service.fetchChunk(uuid, range, 500);

        assertThat(chunk.rendition()).isNotNull();
        assertThat(chunk.rendition().getHeight()).isLessThanOrEqualTo(500);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-10: fetchChunk использует defaultHeight")
    void fetchChunk_usesDefaultHeight() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntityWithRenditions(uuid);
        entity.setDefaultHeight(720);

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));
        when(videoObjectStorage.openProcessed(anyString(), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-99", 1000);
        StreamChunk chunk = service.fetchChunk(uuid, range, null);

        assertThat(chunk.rendition().getHeight()).isEqualTo(720);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-11: fetchChunk выбрасывает для range за пределами файла")
    void fetchChunk_throwsForRangeBeyondFile() {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntity(uuid);
        entity.setContentSize(1000L);

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=2000-2999", 1000);

        assertThatThrownBy(() -> service.fetchChunk(uuid, range, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-12: deleteVideo удаляет все renditions")
    void deleteVideo_deletesAllRenditions() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntityWithRenditions(uuid);

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));

        service.deleteVideo(uuid);

        verify(videoObjectStorage, times(3)).deleteProcessed(anyString());
        verify(fileMetadataRepository).delete(entity);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-13: deleteVideo удаляет файл без renditions")
    void deleteVideo_deletesFileWithoutRenditions() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntity(uuid);
        entity.setRenditions(List.of());

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));

        service.deleteVideo(uuid);

        verify(videoObjectStorage).deleteProcessed(uuid.toString());
        verify(fileMetadataRepository).delete(entity);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-15: enqueueUpload отправляет command в Kafka")
    void enqueueUpload_sendsKafkaCommand() throws Exception {
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(mockFile.getSize()).thenReturn(1000L);
        when(uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());

        UUID jobId = service.enqueueUpload(mockFile, "Test", "Desc", 1L, "User");

        ArgumentCaptor<TranscodingCommand> captor = ArgumentCaptor.forClass(TranscodingCommand.class);
        verify(kafkaTemplate).send(anyString(), eq(jobId.toString()), captor.capture());

        TranscodingCommand command = captor.getValue();
        assertThat(command.jobId()).isEqualTo(jobId);
        assertThat(command.videoName()).isEqualTo("Test");
        assertThat(command.userId()).isEqualTo(1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-16: rebuildEmbeddings нормализует векторы")
    void rebuildEmbeddings_normalizesVectors() {
        FileMetadataEntity entity = createEntity(UUID.randomUUID());
        entity.setName("Test");

        when(fileMetadataRepository.findAll()).thenReturn(List.of(entity));
        when(embeddingClient.embed(anyString()))
                .thenReturn(new EmbeddingResponse(List.of(3.0f, 4.0f)));
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rebuildEmbeddings();

        ArgumentCaptor<FileMetadataEntity> captor = ArgumentCaptor.forClass(FileMetadataEntity.class);
        verify(fileMetadataRepository).save(captor.capture());

        float[] combined = captor.getValue().getCombinedEmbedding();
        double norm = 0.0;
        for (float v : combined) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-17: rebuildEmbeddings обрабатывает пустые embeddings")
    void rebuildEmbeddings_handlesEmptyEmbeddings() {
        FileMetadataEntity entity = createEntity(UUID.randomUUID());

        when(fileMetadataRepository.findAll()).thenReturn(List.of(entity));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of()));
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int result = service.rebuildEmbeddings();

        assertThat(result).isEqualTo(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-18: rebuildEmbeddings логирует прогресс")
    void rebuildEmbeddings_logsProgress() {
        List<FileMetadataEntity> entities = List.of(
                createEntity(UUID.randomUUID()),
                createEntity(UUID.randomUUID())
        );

        when(fileMetadataRepository.findAll()).thenReturn(entities);
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int result = service.rebuildEmbeddings();

        assertThat(result).isEqualTo(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-19: getVideoList возвращает список")
    void getVideoList_returnsList() {
        when(fileMetadataRepository.findAllByUploaderId(1L))
                .thenReturn(List.of(createEntity(UUID.randomUUID())));

        List<FileMetadataEntity> result = service.getVideoList(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-20: getVideoList без userId возвращает все")
    void getVideoList_withoutUserId() {
        when(fileMetadataRepository.findAll())
                .thenReturn(List.of(
                        createEntity(UUID.randomUUID()),
                        createEntity(UUID.randomUUID())
                ));

        List<FileMetadataEntity> result = service.getVideoList();

        assertThat(result).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-21: handleTranscodingResult использует defaultHeight если не указан")
    void handleTranscodingResult_usesDefaultHeightWhenNull() {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test", 1L, "User");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb.jpg",
                null
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        ArgumentCaptor<FileMetadataEntity> captor = ArgumentCaptor.forClass(FileMetadataEntity.class);
        verify(fileMetadataRepository).save(captor.capture());

        assertThat(captor.getValue().getDefaultHeight()).isEqualTo(720);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-22: fetchChunk читает указанный диапазон")
    void fetchChunk_readsSpecificRange() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntity(uuid);
        entity.setContentSize(10000L);

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));
        when(videoObjectStorage.openProcessed(anyString(), eq(1000L), eq(1000L)))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=1000-1999", 1000);
        StreamChunk chunk = service.fetchChunk(uuid, range, null);

        verify(videoObjectStorage).openProcessed(anyString(), eq(1000L), eq(1000L));
        assertThat(chunk.chunk()).hasSize(1000);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-23: handleTranscodingResult игнорирует ошибку LLM")
    void handleTranscodingResult_ignoresLlmError() {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test", 1L, "User");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenThrow(new RuntimeException("LLM error"));
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleTranscodingResult(result);

        verify(fileMetadataRepository).save(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-24: handleTranscodingResult игнорирует ошибку удаления raw")
    void handleTranscodingResult_ignoresDeleteRawError() throws Exception {
        UUID jobId = UUID.randomUUID();
        UploadJobEntity job = createUploadJob(jobId, "Test", 1L, "User");
        job.setStorageKey("raw/test.mp4");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb.jpg",
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(embeddingClient.embed(anyString())).thenReturn(new EmbeddingResponse(List.of(0.1f)));
        when(videoLlmService.enrich(anyString(), anyString())).thenReturn(VideoLlmResult.empty());
        when(fileMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Delete failed")).when(videoObjectStorage).deleteProcessed("raw/test.mp4");

        service.handleTranscodingResult(result);

        verify(fileMetadataRepository).save(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VSTREAM-COMPLEX-25: fetchChunk возвращает правильный contentType")
    void fetchChunk_returnsCorrectContentType() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = createEntity(uuid);
        entity.setContentType("video/mp4");

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));
        when(videoObjectStorage.openProcessed(anyString(), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-99", 1000);
        StreamChunk chunk = service.fetchChunk(uuid, range, null);

        assertThat(chunk.contentType()).isEqualTo("video/mp4");
    }

    private FileMetadataEntity createEntity(UUID uuid) {
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(1L);
        entity.setUuid(uuid);
        entity.setName("Test Video");
        entity.setContentSize(10000L);
        entity.setContentType("video/mp4");
        return entity;
    }

    private FileMetadataEntity createEntityWithRenditions(UUID uuid) {
        FileMetadataEntity entity = createEntity(uuid);

        VideoRenditionEntity r360 = new VideoRenditionEntity();
        r360.setId(1L);
        r360.setFile(entity);
        r360.setHeight(360);
        r360.setStorageKey("key360");
        r360.setContentSize(1000L);
        r360.setContentType("video/mp4");

        VideoRenditionEntity r720 = new VideoRenditionEntity();
        r720.setId(2L);
        r720.setFile(entity);
        r720.setHeight(720);
        r720.setStorageKey("key720");
        r720.setContentSize(2000L);
        r720.setContentType("video/mp4");

        VideoRenditionEntity r1080 = new VideoRenditionEntity();
        r1080.setId(3L);
        r1080.setFile(entity);
        r1080.setHeight(1080);
        r1080.setStorageKey("key1080");
        r1080.setContentSize(3000L);
        r1080.setContentType("video/mp4");

        entity.setRenditions(List.of(r360, r720, r1080));
        return entity;
    }

    private UploadJobEntity createUploadJob(UUID id, String name, Long userId, String displayName) {
        UploadJobEntity job = new UploadJobEntity();
        job.setId(id);
        job.setVideoName(name);
        job.setUserId(userId);
        job.setDisplayName(displayName);
        job.setStage(TranscodingJobStage.TRANSCODING);
        return job;
    }
}
