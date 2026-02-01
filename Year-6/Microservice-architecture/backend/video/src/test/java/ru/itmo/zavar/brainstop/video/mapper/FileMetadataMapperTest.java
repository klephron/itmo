package ru.itmo.zavar.brainstop.video.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.model.VideoRenditionEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class FileMetadataMapperTest {

    private FileMetadataMapper mapper;
    private FileMetadataEntity testEntity;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(FileMetadataMapper.class);

        testEntity = new FileMetadataEntity();
        testEntity.setId(1L);
        testEntity.setUuid(UUID.randomUUID());
        testEntity.setName("Test Video");
        testEntity.setContentType("video/mp4");
        testEntity.setContentSize(10000L);
        testEntity.setUploaderId(1L);
        testEntity.setUploaderName("TestUser");
        testEntity.setUploadedAt(LocalDateTime.now());
        testEntity.setLikesCount(10L);
        testEntity.setViewsCount(100L);
        testEntity.setDefaultHeight(720);
        testEntity.setDescription("Test description");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-01: toDto конвертирует entity в DTO")
    void toDto_convertsEntityToDto() {
        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(testEntity.getId());
        assertThat(dto.uuid()).isEqualTo(testEntity.getUuid());
        assertThat(dto.name()).isEqualTo(testEntity.getName());
        assertThat(dto.contentSize()).isEqualTo(testEntity.getContentSize());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-02: toDto мапит availableQualities")
    void toDto_mapsAvailableQualities() {
        VideoRenditionEntity r1 = createRendition(360);
        VideoRenditionEntity r2 = createRendition(720);
        VideoRenditionEntity r3 = createRendition(1080);
        testEntity.setRenditions(List.of(r1, r2, r3));

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.availableQualities()).containsExactly(360, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-03: toDto с пустым списком renditions")
    void toDto_withEmptyRenditions() {
        testEntity.setRenditions(List.of());

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.availableQualities()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-04: toDto с null renditions")
    void toDto_withNullRenditions() {
        testEntity.setRenditions(null);

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.availableQualities()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-06: toDto список entities")
    void toDto_convertsListOfEntities() {
        FileMetadataEntity entity2 = new FileMetadataEntity();
        entity2.setId(2L);
        entity2.setUuid(UUID.randomUUID());
        entity2.setName("Video 2");
        entity2.setContentSize(2000L);
        entity2.setUploadedAt(LocalDateTime.now());

        List<FileMetadataDto> dtos = mapper.toDto(List.of(testEntity, entity2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).name()).isEqualTo("Test Video");
        assertThat(dtos.get(1).name()).isEqualTo("Video 2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-07: toDto с null значениями")
    void toDto_withNullFields() {
        testEntity.setDescription(null);
        testEntity.setThumbnailKey(null);
        testEntity.setDefaultHeight(null);

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.description()).isNull();
        assertThat(dto.thumbnailKey()).isNull();
        assertThat(dto.defaultHeight()).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-09: toDto сортирует availableQualities")
    void toDto_sortsAvailableQualities() {
        VideoRenditionEntity r1 = createRendition(1080);
        VideoRenditionEntity r2 = createRendition(360);
        VideoRenditionEntity r3 = createRendition(720);
        testEntity.setRenditions(List.of(r1, r2, r3));

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.availableQualities()).containsExactly(360, 720, 1080);
        assertThat(dto.availableQualities()).isSorted();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-10: toDto с дублирующимися качествами")
    void toDto_withDuplicateQualities() {
        VideoRenditionEntity r1 = createRendition(720);
        VideoRenditionEntity r2 = createRendition(720);
        testEntity.setRenditions(List.of(r1, r2));

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.availableQualities()).containsExactly(720, 720);
    }

    @ParameterizedTest
    @ValueSource(ints = {144, 240, 360, 480, 720, 1080, 1440, 2160, 4320})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-11: toDto мапит различные высоты")
    void toDto_mapsDifferentHeights(int height) {
        VideoRenditionEntity r = createRendition(height);
        testEntity.setRenditions(List.of(r));

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.availableQualities()).contains(height);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-13: toDto с максимальными значениями")
    void toDto_withMaxValues() {
        testEntity.setContentSize(Long.MAX_VALUE);
        testEntity.setLikesCount(Long.MAX_VALUE);
        testEntity.setViewsCount(Long.MAX_VALUE);

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.contentSize()).isEqualTo(Long.MAX_VALUE);
        assertThat(dto.likesCount()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-14: toDto с очень длинным именем")
    void toDto_withVeryLongName() {
        String longName = "a".repeat(1000);
        testEntity.setName(longName);

        FileMetadataDto dto = mapper.toDto(testEntity);

        assertThat(dto.name()).isEqualTo(longName);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-15: toDto с пустым списком")
    void toDto_withEmptyList() {
        List<FileMetadataDto> dtos = mapper.toDto(new ArrayList<>());

        assertThat(dtos).isEmpty();
    }

    private VideoRenditionEntity createRendition(int height) {
        VideoRenditionEntity rendition = new VideoRenditionEntity();
        rendition.setId((long) height);
        rendition.setHeight(height);
        rendition.setContentType("video/mp4");
        rendition.setContentSize((long) (height * 1000));
        rendition.setStorageKey("renditions/" + height + "p.mp4");
        rendition.setFile(testEntity);
        return rendition;
    }
}
