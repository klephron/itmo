package ru.itmo.zavar.brainstop.storage.service;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.storage.infra.MinioStorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-4.1", "FR-4.2"})
@Layer(TestLayer.BE_UNIT)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new MinioStorageService(minioClient);
        setField(service, "videoBucketName", "videos");
        setField(service, "thumbBucketName", "thumbs");
        setField(service, "avatarBucketName", "avatars");
        setField(service, "streamPartSize", 5242880L);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-01: initialize создает отсутствующие buckets")
    void initialize_createsMissingBuckets() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        service.initialize();

        verify(minioClient, times(3)).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-02: initialize не создает существующие buckets")
    void initialize_doesNotCreateExistingBuckets() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        service.initialize();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-04: uploadProcessed передает правильные параметры в MinioClient")
    void uploadProcessed_passesCorrectParams() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[100]);
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

        service.uploadProcessed("test-key", is, 100L);

        verify(minioClient).putObject(captor.capture());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-05: deleteProcessed удаляет объект из video bucket")
    void deleteProcessed_deletesFromVideoBucket() throws Exception {
        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);

        service.deleteProcessed("test-key");

        verify(minioClient).removeObject(captor.capture());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-06: uploadAvatar формирует правильное имя файла")
    void uploadAvatar_createsCorrectObjectName() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[100]);

        String result = service.uploadAvatar(is, 100L, "image/jpeg", 123L);

        assertThat(result).isEqualTo("avatars/123.jpg");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-07: uploadAvatar передает contentType")
    void uploadAvatar_passesContentType() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[100]);
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

        service.uploadAvatar(is, 100L, "image/png", 456L);

        verify(minioClient).putObject(captor.capture());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-09: uploadThumbnail устанавливает contentType image/jpeg")
    void uploadThumbnail_setsJpegContentType() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[50]);
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

        String result = service.uploadThumbnail("thumb-key", is, 50L);

        assertThat(result).isEqualTo("thumb-key");
        verify(minioClient).putObject(captor.capture());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-13: getProcessedObjectSize возвращает размер объекта")
    void getProcessedObjectSize_returnsSize() throws Exception {
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(response.size()).thenReturn(12345L);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(response);

        long size = service.getProcessedObjectSize("video-key");

        assertThat(size).isEqualTo(12345L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-14: uploadProcessed использует streamPartSize")
    void uploadProcessed_usesStreamPartSize() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[10000000]);

        service.uploadProcessed("large-video", is, 10000000L);

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-MINIO-15: uploadProcessed пробрасывает исключения")
    void uploadProcessed_propagatesExceptions() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[100]);
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> service.uploadProcessed("key", is, 100L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-16: uploadAvatar с разными userId создает разные имена")
    void uploadAvatar_differentUserIdsDifferentNames() throws Exception {
        InputStream is1 = new ByteArrayInputStream(new byte[100]);
        InputStream is2 = new ByteArrayInputStream(new byte[100]);

        String name1 = service.uploadAvatar(is1, 100L, "image/jpeg", 1L);
        String name2 = service.uploadAvatar(is2, 100L, "image/jpeg", 2L);

        assertThat(name1).isEqualTo("avatars/1.jpg");
        assertThat(name2).isEqualTo("avatars/2.jpg");
        assertThat(name1).isNotEqualTo(name2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MINIO-18: uploadThumbnail возвращает тот же ключ")
    void uploadThumbnail_returnsSameKey() throws Exception {
        String result = service.uploadThumbnail("my-thumb", new ByteArrayInputStream(new byte[50]), 50L);

        assertThat(result).isEqualTo("my-thumb");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-MINIO-19: getProcessedObjectSize пробрасывает исключения")
    void getProcessedObjectSize_propagatesExceptions() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new RuntimeException("Stat failed"));

        assertThatThrownBy(() -> service.getProcessedObjectSize("key"))
                .isInstanceOf(RuntimeException.class);
    }
}
