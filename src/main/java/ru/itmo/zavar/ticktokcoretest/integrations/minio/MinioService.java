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
        String[] buckets = new String[]{videoBucketName, thumbBucketName, avatarBucketName};
        for (String bucket : buckets) {
            if (!this.bucketExists(bucket)) {
                this.createBucket(bucket);
            }
        }
    }

    private boolean bucketExists(String name) {
        try {
            return this.minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(name).build());
        } catch (Exception e) {
            return false;
        }
    }

    private void createBucket(String name) {
        try {
            this.minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(name).build());
        } catch (Exception e) {
            throw new RuntimeException("Cannot create MinIO bucket: " + e.getMessage(), e);
        }
    }

    public void uploadVideoFile(MultipartFile file, FileMetadataEntity fileMetadataEntity) throws Exception {
        this.minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(this.videoBucketName)
                        .object(fileMetadataEntity.getUuid().toString())
                        .stream(file.getInputStream(), file.getSize(), this.streamPartSize)
                        .build()
        );
    }

    public void uploadVideoFile(InputStream inputStream, long size, FileMetadataEntity fileMetadataEntity) throws Exception {
        this.minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(this.videoBucketName)
                        .object(fileMetadataEntity.getUuid().toString())
                        .stream(inputStream, size, this.streamPartSize)
                        .build()
        );
    }


    public void deleteVideoFile(UUID fileUUID) throws Exception {
        this.minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(this.videoBucketName)
                        .object(fileUUID.toString())
                        .build()
        );
    }

    public String uploadAvatar(InputStream is, long size, String contentType, Long userId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String objectName = "avatars/" + userId + ".jpg";
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(this.avatarBucketName)
                .object(objectName)
                .stream(is, size, -1)
                .contentType(contentType)
                .build();
        this.minioClient.putObject(args);
        return objectName;
    }

    public InputStream downloadAvatar(String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(this.avatarBucketName)
                .object(objectName)
                .build();
        return this.minioClient.getObject(args);
    }

    public String uploadThumbnail(InputStream is, long size, FileMetadataEntity meta) throws Exception {
        String objectName = "thumbnails/" + meta.getUuid() + ".jpg";

        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(this.thumbBucketName)
                .object(objectName)
                .stream(is, size, -1)
                .contentType("image/jpeg")
                .build();

        this.minioClient.putObject(args);
        return objectName;
    }

    public InputStream getVideoFileAsInputStream(FileMetadataEntity fileMetadataEntity, long offset, long length) throws Exception {
        return this.minioClient.getObject(
                GetObjectArgs
                        .builder()
                        .bucket(this.videoBucketName)
                        .offset(offset)
                        .length(length)
                        .object(fileMetadataEntity.getUuid().toString())
                        .build()
        );
    }

    public InputStream getThumbnailAsInputStream(FileMetadataEntity fileMetadataEntity) throws Exception {
        String key = fileMetadataEntity.getThumbnailKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("No thumbnail key for file " + fileMetadataEntity.getUuid());
        }

        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(this.thumbBucketName)
                        .object(key)
                        .build()
        );
    }

    public InputStream downloadVideoFile(FileMetadataEntity meta) throws Exception {
        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(this.videoBucketName)
                        .object(meta.getUuid().toString())
                        .build()
        );
    }
}

