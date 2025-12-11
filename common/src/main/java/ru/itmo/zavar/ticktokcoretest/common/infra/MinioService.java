package ru.itmo.zavar.ticktokcoretest.common.infra;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.infra.VideoObjectStorage;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "minio", name = "url")
// MSPLIT: MinIO adapter for future storage abstraction
public class MinioService implements VideoObjectStorage {

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

    @Override
    public void uploadProcessed(String key, InputStream is, long size) throws Exception {
        this.minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(this.videoBucketName)
                        .object(key)
                        .stream(is, size, this.streamPartSize)
                        .build()
        );
    }

    @Override
    public void deleteProcessed(String key) throws Exception {
        this.minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(this.videoBucketName)
                        .object(key)
                        .build()
        );
    }

    @Override
    public String uploadAvatar(InputStream is, long size, String contentType, Long userId) throws Exception {
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

    @Override
    public InputStream downloadAvatar(String objectName) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(this.avatarBucketName)
                .object(objectName)
                .build();
        return this.minioClient.getObject(args);
    }

    @Override
    public String uploadThumbnail(String key, InputStream is, long size) throws Exception {
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(this.thumbBucketName)
                .object(key)
                .stream(is, size, -1)
                .contentType("image/jpeg")
                .build();

        this.minioClient.putObject(args);
        return key;
    }

    @Override
    public InputStream openProcessed(String key, long offset, long length) throws Exception {
        return this.minioClient.getObject(
                GetObjectArgs
                        .builder()
                        .bucket(this.videoBucketName)
                        .offset(offset)
                        .length(length)
                        .object(key)
                        .build()
        );
    }

    @Override
    public InputStream openThumbnail(String key) throws Exception {
        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(this.thumbBucketName)
                        .object(key)
                        .build()
        );
    }

    @Override
    public InputStream downloadProcessed(String key) throws Exception {
        return this.minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(this.videoBucketName)
                        .object(key)
                        .build()
        );
    }

}
