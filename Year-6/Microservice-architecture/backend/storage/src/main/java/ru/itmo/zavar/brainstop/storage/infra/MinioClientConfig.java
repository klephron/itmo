package ru.itmo.zavar.brainstop.storage.infra;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "minio", name = "url")
public class MinioClientConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.username}")
    private String minioUsername;

    @Value("${minio.password}")
    private String minioPassword;

    @Bean
    public MinioClient generateMinioClient() {
        try {
            return MinioClient.builder()
                    .endpoint(this.minioUrl)
                    .credentials(this.minioUsername, this.minioPassword)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
