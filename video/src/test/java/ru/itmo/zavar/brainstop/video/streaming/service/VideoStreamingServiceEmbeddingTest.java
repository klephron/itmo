package ru.itmo.zavar.brainstop.video.streaming.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.search.client.SearchEmbeddingClient;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmService;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.transcoding.repo.UploadJobRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoStreamingServiceEmbeddingTest {

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
    private VideoStreamingService service;

    @BeforeEach
    void setUp() {
        service = new VideoStreamingService(
                fileMetadataRepository,
                videoObjectStorage,
                jobRegistry,
                uploadJobRepository,
                embeddingClient,
                kafkaTemplate,
                videoLlmService
        );
    }

    @Test
    @DisplayName("Rebuild embeddings normalizes and saves all videos")
    void rebuildEmbeddings_normalizesAndSaves() {
        FileMetadataEntity first = new FileMetadataEntity();
        first.setUuid(java.util.UUID.randomUUID());
        first.setName("First title");
        first.setDescription("First description");

        FileMetadataEntity second = new FileMetadataEntity();
        second.setUuid(java.util.UUID.randomUUID());
        second.setName("Second title");
        second.setDescription("Second description");

        when(fileMetadataRepository.findAll()).thenReturn(List.of(first, second));
        when(fileMetadataRepository.save(any(FileMetadataEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Return different short vectors depending on text to ensure normalization path is exercised
        AtomicInteger calls = new AtomicInteger();
        when(embeddingClient.embed(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            if (text.contains("First")) {
                return new EmbeddingResponse(List.of(1f, 0f, 0f));
            } else if (text.contains("Second")) {
                return new EmbeddingResponse(List.of(0f, 2f, 0f));
            }
            // combined texts
            calls.incrementAndGet();
            return new EmbeddingResponse(List.of(1f, 1f, 0f));
        });

        ArgumentCaptor<FileMetadataEntity> saved = ArgumentCaptor.forClass(FileMetadataEntity.class);

        int updated = service.rebuildEmbeddings();

        verify(fileMetadataRepository).findAll();
        verify(fileMetadataRepository, times(2)).save(saved.capture());

        for (FileMetadataEntity meta : saved.getAllValues()) {
            assertThat(meta.getTitleEmbedding()).isNotNull();
            assertThat(meta.getDescriptionEmbedding()).isNotNull();
            assertThat(meta.getCombinedEmbedding()).isNotNull();

            assertThat(meta.getTitleEmbedding().length).isEqualTo(1024);
            assertThat(meta.getDescriptionEmbedding().length).isEqualTo(1024);
            assertThat(meta.getCombinedEmbedding().length).isEqualTo(1024);

            double norm = 0.0;
            for (float v : meta.getCombinedEmbedding()) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            assertThat(norm).isBetween(0.99, 1.01);
        }
        verify(embeddingClient, atLeast(6)).embed(anyString());
    }
}
