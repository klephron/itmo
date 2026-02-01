package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToVideoMetadataSummaryMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class VideoMetadataPublicApiImplTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileMetadataToVideoMetadataSummaryMapper videoMetadataSummaryMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private VideoMetadataPublicApiImpl videoMetadataPublicApi;

    private UUID testUuid;
    private FileMetadataEntity testEntity;
    private VideoMetadataSummary testSummary;

    @BeforeEach
    void setUp() {
        testUuid = UUID.randomUUID();
        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(testUuid);
        testEntity.setName("Test Video");

        testSummary = new VideoMetadataSummary(
            1L, testUuid, 1000L, 100L, 10L, "Test Video", "media",
            10000L,"key"
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-01: findByUuid возвращает summary")
    void findByUuid_returnsSummary() {
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(testEntity)).thenReturn(testSummary);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<VideoMetadataSummary> result = videoMetadataPublicApi.findByUuid(testUuid);

        assertThat(result).isPresent();
        assertThat(result.get().uuid()).isEqualTo(testUuid);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-META-02: findByUuid возвращает empty для несуществующего видео")
    void findByUuid_returnsEmptyForNonExistent() {
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.empty());
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<VideoMetadataSummary> result = videoMetadataPublicApi.findByUuid(testUuid);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-03: getRequired выбрасывает исключение если видео не найдено")
    void getRequired_throwsIfNotFound() {
        when(fileMetadataRepository.findByUuid(testUuid)).thenReturn(Optional.empty());
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> videoMetadataPublicApi.getRequired(testUuid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-04: incrementLikesCount увеличивает счетчик")
    void incrementLikesCount_incrementsCounter() {
        testEntity.setLikesCount(10L);
        when(fileMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        long result = videoMetadataPublicApi.incrementLikesCount(1L, 1L);

        assertThat(result).isEqualTo(11L);
        assertThat(testEntity.getLikesCount()).isEqualTo(11L);
        verify(fileMetadataRepository).save(testEntity);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-05: incrementLikesCount уменьшает счетчик")
    void incrementLikesCount_decrementsCounter() {
        testEntity.setLikesCount(10L);
        when(fileMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        long result = videoMetadataPublicApi.incrementLikesCount(1L, -1L);

        assertThat(result).isEqualTo(9L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-06: incrementLikesCount не допускает отрицательных значений")
    void incrementLikesCount_preventsNegativeValues() {
        testEntity.setLikesCount(5L);
        when(fileMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        long result = videoMetadataPublicApi.incrementLikesCount(1L, -10L);

        assertThat(result).isEqualTo(0L);
        assertThat(testEntity.getLikesCount()).isEqualTo(0L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-08: incrementViewsCount увеличивает просмотры")
    void incrementViewsCount_incrementsViews() {
        testEntity.setViewsCount(100L);
        when(fileMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        long result = videoMetadataPublicApi.incrementViewsCount(1L, 5L);

        assertThat(result).isEqualTo(105L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-09: updateLikesCount устанавливает новое значение")
    void updateLikesCount_setsNewValue() {
        when(fileMetadataRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        videoMetadataPublicApi.updateLikesCount(1L, 50L);

        assertThat(testEntity.getLikesCount()).isEqualTo(50L);
        verify(fileMetadataRepository).save(testEntity);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-10: updateViewsCount устанавливает новое значение")
    void updateViewsCount_setsNewValue() {
        when(fileMetadataRepository.findById(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        videoMetadataPublicApi.updateViewsCount(1L, 500L);

        assertThat(testEntity.getViewsCount()).isEqualTo(500L);
        verify(fileMetadataRepository).save(testEntity);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-14: incrementLikesCount обрабатывает null начальное значение")
    void incrementLikesCount_handlesNullInitialValue() {
        testEntity.setLikesCount(null);
        when(fileMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        long result = videoMetadataPublicApi.incrementLikesCount(1L, 5L);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-15: incrementViewsCount обрабатывает null начальное значение")
    void incrementViewsCount_handlesNullInitialValue() {
        testEntity.setViewsCount(null);
        when(fileMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testEntity));
        when(videoMetadataSummaryMapper.toSummary(any())).thenReturn(testSummary);

        long result = videoMetadataPublicApi.incrementViewsCount(1L, 10L);

        assertThat(result).isEqualTo(10L);
    }
}
