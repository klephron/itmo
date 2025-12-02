package ru.itmo.zavar.ticktokcoretest.integrations.s3;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.stream.part-size}")
    private long streamPartSize;

    private final MinioClient minioClient;

    @PostConstruct
    public void initialize() {
        if (!bucketExists()) {
            createBucket();
        }
    }

    private boolean bucketExists() {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
        } catch (Exception e) {
            return false;
        }
    }

    private void createBucket() {
        try {
            MakeBucketArgs args = MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build();
            minioClient.makeBucket(args);
        } catch (Exception e) {
            // В статье они делали exitProcess(1); мы кинем RuntimeException
            throw new RuntimeException("Cannot create MinIO bucket: " + e.getMessage(), e);
        }
    }

    public void uploadFile(MultipartFile file, FileMetadata fileMetadata) throws Exception {
        minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .object(fileMetadata.getUuid().toString())
                        .stream(file.getInputStream(), file.getSize(), streamPartSize)
                        .build()
        );
    }

    public void uploadFile(InputStream inputStream, long size, FileMetadata fileMetadata) throws Exception {
        minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .object(fileMetadata.getUuid().toString())
                        .stream(inputStream, size, streamPartSize)
                        .build()
        );
    }


    public void deleteFile(UUID fileUUID) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileUUID.toString())
                        .build()
        );
    }

    public InputStream getFileAsInputStream(FileMetadata fileMetadata, long offset, long length) throws Exception {
        return minioClient.getObject(
                GetObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .offset(offset)
                        .length(length)
                        .object(fileMetadata.getUuid().toString())
                        .build()
        );
    }
}

