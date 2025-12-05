package ru.itmo.zavar.ticktokcoretest.integrations.minio;

import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    @Value("${minio.bucket.video}")
    private String videoBucketName;

    @Value("${minio.bucket.thumb}")
    private String thumbBucketName;

    @Value("${minio.bucket.avatar}")
    private String avatarBucketName;

    @Value("${minio.stream.part-size}")
    private long streamPartSize;

    private final MinioClient minioClient;

    @PostConstruct
    public void initialize() {
        if (!bucketVideoExists()) {
            createVideoBucket();
        }
        if (!bucketThumbExists()) {
            createThumbBucket();
        }
        if (!bucketAvatarExists()) {
            createAvatarBucket();
        }
    }

    private boolean bucketVideoExists() {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(videoBucketName).build());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean bucketAvatarExists() {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(avatarBucketName).build());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean bucketThumbExists() {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(thumbBucketName).build());
        } catch (Exception e) {
            return false;
        }
    }

    private void createVideoBucket() {
        try {
            MakeBucketArgs args = MakeBucketArgs.builder()
                    .bucket(videoBucketName)
                    .build();
            minioClient.makeBucket(args);
        } catch (Exception e) {
            // В статье они делали exitProcess(1); мы кинем RuntimeException
            throw new RuntimeException("Cannot create MinIO bucket: " + e.getMessage(), e);
        }
    }

    private void createThumbBucket() {
        try {
            MakeBucketArgs args = MakeBucketArgs.builder()
                    .bucket(thumbBucketName)
                    .build();
            minioClient.makeBucket(args);
        } catch (Exception e) {
            // В статье они делали exitProcess(1); мы кинем RuntimeException
            throw new RuntimeException("Cannot create MinIO bucket: " + e.getMessage(), e);
        }
    }

    private void createAvatarBucket() {
        try {
            MakeBucketArgs args = MakeBucketArgs.builder()
                    .bucket(avatarBucketName)
                    .build();
            minioClient.makeBucket(args);
        } catch (Exception e) {
            // В статье они делали exitProcess(1); мы кинем RuntimeException
            throw new RuntimeException("Cannot create MinIO bucket: " + e.getMessage(), e);
        }
    }

    public void uploadFile(MultipartFile file, FileMetadataEntity fileMetadataEntity) throws Exception {
        minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(videoBucketName)
                        .object(fileMetadataEntity.getUuid().toString())
                        .stream(file.getInputStream(), file.getSize(), streamPartSize)
                        .build()
        );
    }

    public void uploadFile(InputStream inputStream, long size, FileMetadataEntity fileMetadataEntity) throws Exception {
        minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(videoBucketName)
                        .object(fileMetadataEntity.getUuid().toString())
                        .stream(inputStream, size, streamPartSize)
                        .build()
        );
    }


    public void deleteFile(UUID fileUUID) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(videoBucketName)
                        .object(fileUUID.toString())
                        .build()
        );
    }

    public String uploadAvatar(InputStream is, long size, String contentType, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String objectName = "avatars/" + userId + ".jpg"; // или UUID
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(avatarBucketName)
                .object(objectName)
                .stream(is, size, -1)
                .contentType(contentType)
                .build();
        minioClient.putObject(args);
        return objectName;
    }

    public InputStream downloadAvatar(String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(avatarBucketName)
                .object(objectName)
                .build();
        return minioClient.getObject(args);
    }

    public String uploadThumbnail(InputStream is, long size, FileMetadataEntity meta) throws Exception {
        // можно использовать тот же bucket, просто другой префикс
        String objectName = "thumbnails/" + meta.getUuid() + ".jpg";

        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(thumbBucketName)
                .object(objectName)
                .stream(is, size, -1)
                .contentType("image/jpeg")
                .build();

        minioClient.putObject(args);
        return objectName; // вернём ключ, чтобы сохранить в БД
    }

    public InputStream getFileAsInputStream(FileMetadataEntity fileMetadataEntity, long offset, long length) throws Exception {
        return minioClient.getObject(
                GetObjectArgs
                        .builder()
                        .bucket(videoBucketName)
                        .offset(offset)
                        .length(length)
                        .object(fileMetadataEntity.getUuid().toString())
                        .build()
        );
    }

    public InputStream getThumbAsInputStream(FileMetadataEntity fileMetadataEntity) throws Exception {
        String key = fileMetadataEntity.getThumbnailKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("No thumbnail key for file " + fileMetadataEntity.getUuid());
        }

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(thumbBucketName)
                        .object(key)
                        .build()
        );
    }

    public InputStream downloadVideo(FileMetadataEntity meta) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(videoBucketName)
                        .object(meta.getUuid().toString())
                        .build()
        );
    }
}

