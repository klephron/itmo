package ru.itmo.zavar.brainstop.video.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.service.FeedService;
import ru.itmo.zavar.brainstop.video.service.FeedSessionService;
import ru.itmo.zavar.brainstop.video.streaming.controller.VideoStreamingController;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.FeedSessionResponse;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.UploadStartResponse;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamChunk;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.3", "FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class VideoStreamingControllerTest {

    @Mock
    private VideoStreamingService videoStreamingService;

    @Mock
    private TranscodingJobRegistry jobRegistry;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataMapper fileMetadataMapper;

    @Mock
    private FeedService feedService;

    @Mock
    private FeedSessionService feedSessionService;

    @Mock
    private VideoObjectStorage videoObjectStorage;

    @InjectMocks
    private VideoStreamingController controller;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private CurrentUser currentUser;

    private UUID testUuid;
    private FileMetadataEntity testEntity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "chunkDefaultSize", 1048576L);

        testUuid = UUID.randomUUID();
        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(testUuid);
        testEntity.setName("Test Video");
        testEntity.setThumbnailKey("thumbnails/test.jpg");
        testEntity.setContentSize(10000L);
        testEntity.setContentType("video/mp4");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-01: fetchChunk возвращает 206 Partial Content")
    void fetchChunk_returnsPartialContent() throws Exception {
        byte[] testData = new byte[1000];
        StreamChunk chunk = new StreamChunk(testEntity, null, testData);

        when(videoStreamingService.fetchChunk(eq(testUuid), any(), eq(null)))
                .thenReturn(chunk);

        ResponseEntity<byte[]> response = controller.fetchChunk("bytes=0-999", testUuid, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).hasSize(1000);
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).contains("video/mp4");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-02: fetchChunk устанавливает правильные заголовки")
    void fetchChunk_setsCorrectHeaders() throws Exception {
        byte[] testData = new byte[1000];
        StreamChunk chunk = new StreamChunk(testEntity, null, testData);

        when(videoStreamingService.fetchChunk(any(), any(), any())).thenReturn(chunk);

        ResponseEntity<byte[]> response = controller.fetchChunk("bytes=0-999", testUuid, null);

        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(headers.getFirst(HttpHeaders.CONTENT_LENGTH)).isNotNull();
        assertThat(headers.getFirst(HttpHeaders.CONTENT_RANGE)).contains("bytes");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-03: fetchChunk с качеством")
    void fetchChunk_withQuality() throws Exception {
        byte[] testData = new byte[500];
        StreamChunk chunk = new StreamChunk(testEntity, null, testData);

        when(videoStreamingService.fetchChunk(eq(testUuid), any(), eq(720)))
                .thenReturn(chunk);

        ResponseEntity<byte[]> response = controller.fetchChunk("bytes=0-499", testUuid, 720);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        verify(videoStreamingService).fetchChunk(eq(testUuid), any(), eq(720));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-04: getThumbnail возвращает изображение")
    void getThumbnail_returnsImage() throws Exception {
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.of(testEntity));
        when(videoObjectStorage.openThumbnail("thumbnails/test.jpg"))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        ResponseEntity<Resource> response = controller.getThumbnail(testUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("image/jpeg");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-05: getThumbnail возвращает 404 если видео не найдено")
    void getThumbnail_returns404IfVideoNotFound() {
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getThumbnail(testUuid))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-06: getThumbnail возвращает 404 если thumbnailKey null")
    void getThumbnail_returns404IfThumbnailKeyNull() throws Exception {
        testEntity.setThumbnailKey(null);
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.of(testEntity));

        ResponseEntity<Resource> response = controller.getThumbnail(testUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-07: getThumbnail возвращает 404 если файл не найден")
    void getThumbnail_returns404IfFileNotFound() throws Exception {
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.of(testEntity));
        when(videoObjectStorage.openThumbnail(anyString())).thenThrow(new FileNotFoundException());

        ResponseEntity<Resource> response = controller.getThumbnail(testUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-09: getVideoFeed возвращает ленту для анонимного пользователя")
    void getVideoFeed_returnsForAnonymous() {
        Page<FeedVideoDto> page = new PageImpl<>(List.of());
        when(feedService.getFeed(eq(null), any())).thenReturn(page);

        ResponseEntity<Page<FeedVideoDto>> response = controller.getVideoFeed(0, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedService).getFeed(eq(null), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-10: getVideoFeed возвращает ленту для авторизованного пользователя")
    void getVideoFeed_returnsForAuthenticatedUser() {
        Page<FeedVideoDto> page = new PageImpl<>(List.of());
        when(feedService.getFeed(eq(1L), any())).thenReturn(page);
        when(currentUser.getId()).thenReturn(1L);

        ResponseEntity<Page<FeedVideoDto>> response = controller.getVideoFeed(0, 10, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedService).getFeed(eq(1L), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-11: startFeedSession создает сессию")
    void startFeedSession_createsSession() {
        FeedSessionResponse sessionResponse = new FeedSessionResponse("feed-id", List.of(), false, false, 0L);
        when(feedSessionService.startSession(eq("device-1"), eq(1L), eq(10)))
                .thenReturn(sessionResponse);
        when(currentUser.getId()).thenReturn(1L);

        ResponseEntity<FeedSessionResponse> response = controller.startFeedSession(10, "device-1", currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().feedId()).isEqualTo("feed-id");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-12: startFeedSession требует Device-Id")
    void startFeedSession_requiresDeviceId() {
        assertThatThrownBy(() -> controller.startFeedSession(10, null, currentUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-13: nextFeedItems возвращает следующие элементы")
    void nextFeedItems_returnsNextItems() {
        FeedSessionResponse response = new FeedSessionResponse("feed-id", List.of(), true, false, 0L);
        when(feedSessionService.next(eq("feed-id"), eq("device-1"), eq(1L), eq(1)))
                .thenReturn(response);
        when(currentUser.getId()).thenReturn(1L);

        ResponseEntity<FeedSessionResponse> result = controller.nextFeedItems(
                "feed-id", 1, "device-1", currentUser
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedSessionService).next("feed-id", "device-1", 1L, 1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-14: prevFeedItems возвращает предыдущие элементы")
    void prevFeedItems_returnsPrevItems() {
        FeedSessionResponse response = new FeedSessionResponse("feed-id", List.of(), false, false, 0L);
        when(feedSessionService.prev(eq("feed-id"), eq("device-1"), eq(1L)))
                .thenReturn(response);
        when(currentUser.getId()).thenReturn(1L);

        ResponseEntity<FeedSessionResponse> result = controller.prevFeedItems(
                "feed-id", "device-1", currentUser
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedSessionService).prev("feed-id", "device-1", 1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-15: getMyVideoList требует авторизации")
    void getMyVideoList_requiresAuth() {
        assertThatThrownBy(() -> controller.getMyVideoList(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-18: startUpload требует авторизации")
    void startUpload_requiresAuth() {
        assertThatThrownBy(() -> controller.startUpload(mockFile, "Video", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-19: getUploadStatus возвращает статус")
    void getUploadStatus_returnsStatus() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        );
        when(jobRegistry.get(jobId)).thenReturn(Optional.of(status));

        ResponseEntity<TranscodingJobStatus> response = controller.getUploadStatus(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPercent()).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-20: getUploadStatus возвращает 404 для несуществующего job")
    void getUploadStatus_returns404ForNonExistent() {
        UUID jobId = UUID.randomUUID();
        when(jobRegistry.get(jobId)).thenReturn(Optional.empty());

        ResponseEntity<TranscodingJobStatus> response = controller.getUploadStatus(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-21: deleteVideo удаляет видео")
    void deleteVideo_deletesVideo() throws Exception {
        ResponseEntity<Void> response = controller.deleteVideo(testUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(videoStreamingService).deleteVideo(testUuid);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-22: getActiveUploadJobs возвращает активные загрузки")
    void getActiveUploadJobs_returnsActiveJobs() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 30, "Processing", "video.mp4", 1L
        );
        when(jobRegistry.getActiveJobsForUser(1L)).thenReturn(Map.of(jobId, status));
        when(currentUser.getId()).thenReturn(1L);

        ResponseEntity<?> response = controller.getActiveUploadJobs(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(jobRegistry).getActiveJobsForUser(1L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-STREAM-23: getActiveUploadJobs требует авторизации")
    void getActiveUploadJobs_requiresAuth() {
        assertThatThrownBy(() -> controller.getActiveUploadJobs(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-24: fetchChunk без Range заголовка")
    void fetchChunk_withoutRangeHeader() throws Exception {
        byte[] testData = new byte[1000];
        StreamChunk chunk = new StreamChunk(testEntity, null, testData);

        when(videoStreamingService.fetchChunk(any(), any(), any())).thenReturn(chunk);

        ResponseEntity<byte[]> response = controller.fetchChunk(null, testUuid, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-STREAM-25: startFeedSession для анонимного пользователя")
    void startFeedSession_forAnonymousUser() {
        FeedSessionResponse sessionResponse = new FeedSessionResponse("feed-id", List.of(), false, false, 0L);
        when(feedSessionService.startSession(eq("device-1"), eq(null), eq(10)))
                .thenReturn(sessionResponse);

        ResponseEntity<FeedSessionResponse> response = controller.startFeedSession(10, "device-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(feedSessionService).startSession("device-1", null, 10);
    }
}
