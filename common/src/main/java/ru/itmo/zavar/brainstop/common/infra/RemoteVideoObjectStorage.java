package ru.itmo.zavar.brainstop.common.infra;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.dto.storage.ObjectKeyResponse;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public class RemoteVideoObjectStorage implements VideoObjectStorage {

    private static final String STORAGE_ROOT = "/internal/storage";
    private final WebClient webClient;

    public RemoteVideoObjectStorage(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void uploadProcessed(String key, InputStream is, long size) throws Exception {
        withRetry(this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/processed").queryParam("key", key).build())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(size)
                .body(BodyInserters.fromResource(new SizedInputStreamResource(is, size)))
                .retrieve()
                .bodyToMono(Void.class)).block();
    }

    @Override
    public InputStream openProcessed(String key, long offset, long length) throws Exception {
        byte[] data = withRetry(this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/processed").queryParam("key", key).build())
                .header("X-Range-Start", Long.toString(offset))
                .header("X-Range-Length", Long.toString(length))
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class)).block();
        if (data == null) {
            throw new IOException("Empty body from storage service");
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public InputStream downloadProcessed(String key) throws Exception {
        byte[] data = withRetry(this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/processed/download").queryParam("key", key).build())
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class)).block();
        if (data == null) {
            throw new IOException("Empty body from storage service");
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public String uploadThumbnail(String key, InputStream is, long size) throws Exception {
        ObjectKeyResponse response = withRetry(this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/thumbnails").queryParam("key", key).build())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(size)
                .body(BodyInserters.fromResource(new SizedInputStreamResource(is, size)))
                .retrieve()
                .bodyToMono(ObjectKeyResponse.class)).block();
        return response != null ? response.key() : null;
    }

    @Override
    public InputStream openThumbnail(String key) throws Exception {
        try {
            byte[] data = withRetry(this.webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/thumbnails").queryParam("key", key).build())
                    .accept(MediaType.IMAGE_JPEG)
                    .retrieve()
                    .bodyToMono(byte[].class)).block();
            if (data == null) {
                throw new IOException("Empty body from storage service");
            }
            return new ByteArrayInputStream(data);
        } catch (WebClientResponseException.NotFound e) {
            throw new FileNotFoundException("Thumbnail not found: " + key);
        }
    }

    @Override
    public void deleteProcessed(String key) throws Exception {
        withRetry(this.webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/processed").queryParam("key", key).build())
                .retrieve()
                .bodyToMono(Void.class)).block();
    }

    @Override
    public String uploadAvatar(InputStream is, long size, String contentType, Long userId) throws Exception {
        ObjectKeyResponse response = withRetry(this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/avatars").queryParam("userId", userId).build())
                .contentType(MediaType.parseMediaType(
                        StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .contentLength(size)
                .body(BodyInserters.fromResource(new SizedInputStreamResource(is, size)))
                .retrieve()
                .bodyToMono(ObjectKeyResponse.class)).block();
        return response != null ? response.key() : null;
    }

    @Override
    public InputStream downloadAvatar(String objectName) throws Exception {
        byte[] data = withRetry(this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(STORAGE_ROOT + "/avatars").queryParam("key", objectName).build())
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class)).block();
        if (data == null) {
            throw new IOException("Empty body from storage service");
        }
        return new ByteArrayInputStream(data);
    }

    private static final class SizedInputStreamResource extends InputStreamResource {
        private final long knownSize;

        private SizedInputStreamResource(InputStream inputStream, long knownSize) {
            super(inputStream);
            this.knownSize = knownSize;
        }

        @Override
        public long contentLength() {
            return this.knownSize;
        }
    }

    private <T> Mono<T> withRetry(Mono<T> mono) {
        return mono.retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500))
                .filter(err -> err instanceof IOException
                        || err instanceof WebClientRequestException
                        || (err instanceof WebClientResponseException resp && resp.getStatusCode().is5xxServerError())));
    }
}
