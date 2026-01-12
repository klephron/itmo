package ru.itmo.zavar.brainstop.search.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class SearchInternalClientFallbackFactoryTest {

    private SearchInternalClientFallbackFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SearchInternalClientFallbackFactory();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-01: Fallback для searchVideos возвращает пустую страницу")
    void fallback_searchVideos() {
        SearchInternalClient fallback = factory.create(new RuntimeException("test"));

        PageResponse<FileMetadataDto> result = fallback.searchVideos("[0.1,0.2]", 0, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-02: Fallback для suggest возвращает пустой список")
    void fallback_suggest() {
        SearchInternalClient fallback = factory.create(new RuntimeException("test"));

        List<FileMetadataDto> result = fallback.suggest("[0.1,0.2]", 5);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-03: Fallback с null причиной")
    void fallback_nullCause() {
        SearchInternalClient fallback = factory.create(null);

        PageResponse<FileMetadataDto> result = fallback.searchVideos("[0.1,0.2]", 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-04: Fallback с разными параметрами страницы")
    void fallback_differentPageParams() {
        SearchInternalClient fallback = factory.create(new RuntimeException("test"));

        PageResponse<FileMetadataDto> result = fallback.searchVideos("[0.1,0.2]", 2, 20);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
    }
}
