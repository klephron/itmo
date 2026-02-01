package ru.itmo.zavar.brainstop.storage.controller;

import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.dto.storage.ObjectKeyResponse;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.storage.infra.MinioStorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-4.1", "FR-4.2"})
@Layer(TestLayer.BE_UNIT)
class StorageControllerTest {

    @Mock
    private VideoObjectStorage storage;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletInputStream inputStream;

    @InjectMocks
    private StorageController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-01: uploadProcessed вызывает storage с правильными параметрами")
    void uploadProcessed_callsStorageWithCorrectParams() throws Exception {
        when(request.getContentLengthLong()).thenReturn(1024L);
        when(request.getInputStream()).thenReturn(inputStream);

        ResponseEntity<Void> response = controller.uploadProcessed("test-key", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(storage).uploadProcessed(eq("test-key"), eq(inputStream), eq(1024L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-02: uploadProcessed использует X-Object-Size если Content-Length отсутствует")
    void uploadProcessed_usesObjectSizeHeader() throws Exception {
        when(request.getContentLengthLong()).thenReturn(-1L);
        when(request.getHeader("X-Object-Size")).thenReturn("2048");

        controller.uploadProcessed("test-key", request);

        verify(storage).uploadProcessed(eq("test-key"), any(), eq(2048L));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STOR-CTL-03: uploadProcessed выбрасывает исключение если размер неизвестен")
    void uploadProcessed_throwsWhenSizeUnknown() {
        when(request.getContentLengthLong()).thenReturn(-1L);
        when(request.getHeader("X-Object-Size")).thenReturn(null);

        assertThatThrownBy(() -> controller.uploadProcessed("test-key", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content-Length is required");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-04: openProcessed возвращает partial content с правильными headers")
    void openProcessed_returnsPartialContentWithHeaders() throws Exception {
        InputStream mockStream = new ByteArrayInputStream(new byte[100]);
        when(storage.openProcessed("test-key", 0, 100)).thenReturn(mockStream);

        ResponseEntity<Resource> response = controller.openProcessed("test-key", 0, 100);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH)).isEqualTo("100");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-99/*");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        verify(storage).openProcessed("test-key", 0, 100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-05: openProcessed корректно вычисляет диапазон")
    void openProcessed_calculatesRangeCorrectly() throws Exception {
        when(storage.openProcessed(anyString(), anyLong(), anyLong()))
                .thenReturn(new ByteArrayInputStream(new byte[500]));

        ResponseEntity<Resource> response = controller.openProcessed("key", 1000, 500);

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 1000-1499/*");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-06: downloadProcessed возвращает весь файл")
    void downloadProcessed_returnsFullFile() throws Exception {
        InputStream mockStream = new ByteArrayInputStream(new byte[1000]);
        when(storage.downloadProcessed("test-key")).thenReturn(mockStream);

        ResponseEntity<Resource> response = controller.downloadProcessed("test-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(storage).downloadProcessed("test-key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-07: downloadProcessed добавляет Content-Length из MinioStorageService")
    void downloadProcessed_addsContentLengthFromMinioService() throws Exception {
        MinioStorageService minioStorage = mock(MinioStorageService.class);
        when(minioStorage.downloadProcessed("test-key")).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(minioStorage.getProcessedObjectSize("test-key")).thenReturn(1000L);

        StorageController controllerWithMinio = new StorageController(minioStorage);
        ResponseEntity<Resource> response = controllerWithMinio.downloadProcessed("test-key");

        assertThat(response.getHeaders().getContentLength()).isEqualTo(1000);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-08: downloadProcessed обрабатывает ошибку получения размера")
    void downloadProcessed_handlesErrorGettingSize() throws Exception {
        MinioStorageService minioStorage = mock(MinioStorageService.class);
        when(minioStorage.downloadProcessed("test-key")).thenReturn(new ByteArrayInputStream(new byte[1000]));
        when(minioStorage.getProcessedObjectSize("test-key")).thenThrow(new RuntimeException("MinIO error"));

        StorageController controllerWithMinio = new StorageController(minioStorage);
        ResponseEntity<Resource> response = controllerWithMinio.downloadProcessed("test-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(-1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-09: deleteProcessed возвращает 204")
    void deleteProcessed_returnsNoContent() throws Exception {
        ResponseEntity<Void> response = controller.deleteProcessed("test-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(storage).deleteProcessed("test-key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-10: uploadThumbnail возвращает ключ объекта")
    void uploadThumbnail_returnsObjectKey() throws Exception {
        when(request.getContentLengthLong()).thenReturn(500L);
        when(storage.uploadThumbnail("thumb-key", inputStream, 500L)).thenReturn("stored-key");
        when(request.getInputStream()).thenReturn(inputStream);

        ResponseEntity<ObjectKeyResponse> response = controller.uploadThumbnail("thumb-key", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-11: openThumbnail возвращает JPEG")
    void openThumbnail_returnsJpeg() throws Exception {
        when(storage.openThumbnail("thumb-key")).thenReturn(new ByteArrayInputStream(new byte[100]));

        ResponseEntity<Resource> response = controller.openThumbnail("thumb-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("image/jpeg");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STOR-CTL-12: openThumbnail возвращает 404 для NoSuchKey")
    void openThumbnail_returns404ForNoSuchKey() throws Exception {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        ErrorResponseException exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(errorResponse);
        when(storage.openThumbnail("missing-key")).thenThrow(exception);

        ResponseEntity<Resource> response = controller.openThumbnail("missing-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STOR-CTL-13: openThumbnail пробрасывает другие ошибки")
    void openThumbnail_throwsOtherErrors() throws Exception {
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("AccessDenied");
        ErrorResponseException exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(errorResponse);
        when(storage.openThumbnail("key")).thenThrow(exception);

        assertThatThrownBy(() -> controller.openThumbnail("key"))
                .isInstanceOf(ErrorResponseException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-14: uploadAvatar использует Content-Type из заголовка")
    void uploadAvatar_usesContentTypeFromHeader() throws Exception {
        when(request.getContentLengthLong()).thenReturn(1024L);
        when(storage.uploadAvatar(inputStream, 1024L, "image/png", 123L)).thenReturn("avatar-key");
        when(request.getInputStream()).thenReturn(inputStream);

        ResponseEntity<ObjectKeyResponse> response = controller.uploadAvatar(123L, "image/png", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(storage).uploadAvatar(inputStream, 1024L, "image/png", 123L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-15: uploadAvatar использует дефолтный Content-Type")
    void uploadAvatar_usesDefaultContentType() throws Exception {
        when(request.getContentLengthLong()).thenReturn(1024L);
        when(storage.uploadAvatar(any(), anyLong(), anyString(), anyLong())).thenReturn("avatar-key");

        controller.uploadAvatar(123L, null, request);

        verify(storage).uploadAvatar(any(), eq(1024L), eq("application/octet-stream"), eq(123L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-16: downloadAvatar возвращает аватар")
    void downloadAvatar_returnsAvatar() throws Exception {
        when(storage.downloadAvatar("avatar-key")).thenReturn(new ByteArrayInputStream(new byte[500]));

        ResponseEntity<Resource> response = controller.downloadAvatar("avatar-key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(storage).downloadAvatar("avatar-key");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-STOR-CTL-17: resolveSize выбрасывает исключение для некорректного X-Object-Size")
    void resolveSize_throwsForInvalidObjectSize() {
        when(request.getContentLengthLong()).thenReturn(-1L);
        when(request.getHeader("X-Object-Size")).thenReturn("invalid");

        assertThatThrownBy(() -> controller.uploadProcessed("key", request))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-18: uploadProcessed обрабатывает нулевой размер")
    void uploadProcessed_handlesZeroSize() throws Exception {
        when(request.getContentLengthLong()).thenReturn(0L);

        controller.uploadProcessed("key", request);

        verify(storage).uploadProcessed(eq("key"), any(), eq(0L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-19: openProcessed с нулевым offset")
    void openProcessed_withZeroOffset() throws Exception {
        when(storage.openProcessed("key", 0, 100)).thenReturn(new ByteArrayInputStream(new byte[100]));

        ResponseEntity<Resource> response = controller.openProcessed("key", 0, 100);

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 0-99/*");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-STOR-CTL-20: uploadAvatar с большим userId")
    void uploadAvatar_withLargeUserId() throws Exception {
        when(request.getContentLengthLong()).thenReturn(500L);
        when(storage.uploadAvatar(any(), anyLong(), anyString(), eq(999999999L))).thenReturn("key");

        ResponseEntity<ObjectKeyResponse> response = controller.uploadAvatar(999999999L, "image/jpeg", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(storage).uploadAvatar(any(), eq(500L), eq("image/jpeg"), eq(999999999L));
    }
}
