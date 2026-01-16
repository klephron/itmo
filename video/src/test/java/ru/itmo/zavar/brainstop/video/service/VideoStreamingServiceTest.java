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
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.client.SearchEmbeddingClient;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmService;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.model.VideoRenditionEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamRange;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;
import ru.itmo.zavar.brainstop.video.transcoding.repo.UploadJobRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.3", "FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class VideoStreamingServiceTest {

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

    @InjectMocks
    private VideoStreamingService videoStreamingService;

    @Mock
    private MultipartFile mockFile;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-01: enqueueUpload создает новый job")
    void enqueueUpload_createsNewJob() throws IOException {
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());

        UUID jobId = videoStreamingService.enqueueUpload(mockFile, "Test Video", "Description", 1L, "User1");

        assertThat(jobId).isNotNull();
        verify(uploadJobRepository).save(any());
        verify(jobRegistry).put(eq(jobId), any(TranscodingJobStatus.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any(TranscodingCommand.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-02: enqueueUpload загружает файл в storage")
    void enqueueUpload_uploadsToStorage() throws Exception {
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(mockFile.getSize()).thenReturn(1000L);

        videoStreamingService.enqueueUpload(mockFile, "Test", null, 1L, "User");

        verify(videoObjectStorage).uploadProcessed(anyString(), any(), eq(1000L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-03: enqueueUpload использует originalFilename если videoName пустое")
    void enqueueUpload_usesOriginalFilename() throws IOException {
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(mockFile.getOriginalFilename()).thenReturn("test-video.mp4");
        when(mockFile.getSize()).thenReturn(1000L);
        videoStreamingService.enqueueUpload(mockFile, null, null, 1L, "User");

        ArgumentCaptor<TranscodingCommand> captor = ArgumentCaptor.forClass(TranscodingCommand.class);
        verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().videoName()).isEqualTo("test-video.mp4");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-04: enqueueUpload возвращает существующий job при дубликате")
    void enqueueUpload_returnsExistingJobForDuplicate() throws IOException {
        UUID existingJobId = UUID.randomUUID();
        var existingJob = new ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity();
        existingJob.setId(existingJobId);

        when(uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.of(existingJob));

        UUID jobId = videoStreamingService.enqueueUpload(mockFile, "Test", null, 1L, "User");

        assertThat(jobId).isEqualTo(existingJobId);
        verify(uploadJobRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-06: handleTranscodingResult удаляет исходник")
    void handleTranscodingResult_deletesRawFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        var job = new ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity();
        job.setId(jobId);
        job.setStorageKey("raw/test.mp4");
        job.setVideoName("Test");
        job.setUserId(1L);
        job.setDisplayName("User");

        TranscodingResult result = new TranscodingResult(
                jobId,
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                null,
                720
        );

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        videoStreamingService.handleTranscodingResult(result);

        verify(videoObjectStorage).deleteProcessed("raw/test.mp4");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STREAM-07: handleTranscodingResult игнорирует null result")
    void handleTranscodingResult_ignoresNullResult() {
        videoStreamingService.handleTranscodingResult(null);

        verify(uploadJobRepository, never()).findById(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STREAM-08: handleTranscodingResult игнорирует неизвестный job")
    void handleTranscodingResult_ignoresUnknownJob() {
        UUID jobId = UUID.randomUUID();
        TranscodingResult result = new TranscodingResult(jobId, List.of(), null, 720);

        when(uploadJobRepository.findById(jobId)).thenReturn(Optional.empty());

        videoStreamingService.handleTranscodingResult(result);

        verify(fileMetadataRepository, never()).save(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-09: getVideoList возвращает список по userId")
    void getVideoList_returnsByUserId() {
        when(fileMetadataRepository.findAllByUploaderId(1L)).thenReturn(List.of(new FileMetadataEntity()));

        List<FileMetadataEntity> result = videoStreamingService.getVideoList(1L);

        assertThat(result).hasSize(1);
        verify(fileMetadataRepository).findAllByUploaderId(1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-10: getVideoList возвращает все видео")
    void getVideoList_returnsAll() {
        when(fileMetadataRepository.findAll()).thenReturn(List.of(
                new FileMetadataEntity(),
                new FileMetadataEntity()
        ));

        List<FileMetadataEntity> result = videoStreamingService.getVideoList();

        assertThat(result).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-11: deleteVideo удаляет метаданные и файлы")
    void deleteVideo_deletesMetadataAndFiles() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setUuid(uuid);

        VideoRenditionEntity rendition = new VideoRenditionEntity();
        rendition.setStorageKey("processed/720p.mp4");
        entity.setRenditions(List.of(rendition));

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));

        videoStreamingService.deleteVideo(uuid);

        verify(videoObjectStorage).deleteProcessed("processed/720p.mp4");
        verify(fileMetadataRepository).delete(entity);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STREAM-12: deleteVideo выбрасывает исключение для несуществующего видео")
    void deleteVideo_throwsForNonExistent() {
        UUID uuid = UUID.randomUUID();
        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoStreamingService.deleteVideo(uuid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STREAM-13: fetchChunk получает чанк из storage")
    void fetchChunk_fetchesFromStorage() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setUuid(uuid);
        entity.setContentSize(10000L);
        entity.setContentType("video/mp4");

        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.of(entity));
        when(videoObjectStorage.openProcessed(anyString(), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-999", 1000);
        var chunk = videoStreamingService.fetchChunk(uuid, range, null);

        assertThat(chunk).isNotNull();
        assertThat(chunk.chunk()).hasSize(1000);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STREAM-14: fetchChunk выбрасывает исключение для несуществующего видео")
    void fetchChunk_throwsForNonExistent() {
        UUID uuid = UUID.randomUUID();
        when(fileMetadataRepository.findWithRenditionsByUuid(uuid)).thenReturn(Optional.empty());

        StreamRange range = StreamRange.parseHttpRangeString(null, 1000);
        assertThatThrownBy(() -> videoStreamingService.fetchChunk(uuid, range, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STREAM-15: enqueueUpload устанавливает ERROR при ошибке загрузки")
    void enqueueUpload_setsErrorOnUploadFailure() throws Exception {
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(uploadJobRepository.findFirstByUserIdAndVideoNameAndStageIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        doThrow(new IOException("Upload failed")).when(videoObjectStorage)
                .uploadProcessed(anyString(), any(), anyLong());

        assertThatThrownBy(() ->
                videoStreamingService.enqueueUpload(mockFile, "Test", null, 1L, "User")
        ).isInstanceOf(IOException.class);

        verify(jobRegistry).update(any(), argThat(updater -> {
            TranscodingJobStatus status = new TranscodingJobStatus(
                    TranscodingJobStage.TRANSCODING, 0, "", null, null
            );
            updater.accept(status);
            return status.getStage() == TranscodingJobStage.ERROR;
        }));
    }
}
