package ru.itmo.zavar.brainstop.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.llm.OpenRouterClient;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmProperties;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceFilterTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;

    @BeforeEach
    void setUp() {
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-01: filterCandidates возвращает все когда disabled")
    void filterCandidates_disabled() {
        List<FileMetadataDto> candidates = createTestCandidates(5);
        List<FileMetadataDto> result = service.filterCandidates("test", candidates);

        assertThat(result).hasSize(5);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-02: filterCandidates с пустым списком")
    void filterCandidates_emptyCandidates() {
        List<FileMetadataDto> result = service.filterCandidates("test", List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-03: filterCandidates с null списком")
    void filterCandidates_nullCandidates() {
        List<FileMetadataDto> result = service.filterCandidates("test", null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-04: filterCandidates фильтрует по UUID")
    void filterCandidates_byUuid() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getFilterMaxTokens()).thenReturn(120);
        when(properties.getFilterMaxItems()).thenReturn(20);
        when(properties.getFilterMaxChars()).thenReturn(2000);
        when(properties.getFilterMaxFieldLength()).thenReturn(140);

        List<FileMetadataDto> candidates = createTestCandidates(5);
        UUID targetUuid = candidates.get(0).uuid();

        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("[\"" + targetUuid + "\"]"));

        List<FileMetadataDto> result = service.filterCandidates("test", candidates);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).uuid()).isEqualTo(targetUuid);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-05: filterCandidates с пустым ответом LLM")
    void filterCandidates_emptyLlmResponse() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getFilterMaxItems()).thenReturn(20);
        when(properties.getFilterMaxChars()).thenReturn(2000);

        List<FileMetadataDto> candidates = createTestCandidates(3);
        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("[]"));

        List<FileMetadataDto> result = service.filterCandidates("test", candidates);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-06: filterCandidates с невалидным JSON")
    void filterCandidates_invalidJson() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getFilterMaxItems()).thenReturn(20);
        when(properties.getFilterMaxChars()).thenReturn(2000);
        when(properties.getFilterMaxFieldLength()).thenReturn(140);

        List<FileMetadataDto> candidates = createTestCandidates(3);
        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("not valid json"));

        List<FileMetadataDto> result = service.filterCandidates("test", candidates);

        assertThat(result).hasSize(3);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-07: filterCandidates с несуществующими UUID")
    void filterCandidates_nonExistentUuids() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getFilterMaxItems()).thenReturn(20);
        when(properties.getFilterMaxChars()).thenReturn(2000);

        List<FileMetadataDto> candidates = createTestCandidates(3);
        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("[\"" + UUID.randomUUID() + "\"]"));

        List<FileMetadataDto> result = service.filterCandidates("test", candidates);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-08: filterCandidates по прямому совпадению")
    void filterCandidates_directMatch() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getShortQueryMaxLength()).thenReturn(4);

        FileMetadataDto match = new FileMetadataDto(
                1L, "Test Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), ""
        );
        FileMetadataDto noMatch = new FileMetadataDto(
                2L, "Other Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), ""
        );

        List<FileMetadataDto> result = service.filterCandidates("Test", List.of(match, noMatch));

        assertThat(result).contains(match);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-09: filterCandidates кеширование")
    void filterCandidates_caching() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getFilterMaxItems()).thenReturn(20);
        when(properties.getFilterMaxChars()).thenReturn(2000);

        List<FileMetadataDto> candidates = createTestCandidates(3);
        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("[\"" + candidates.get(0).uuid() + "\"]"));

        service.filterCandidates("test", candidates);
        List<FileMetadataDto> result = service.filterCandidates("test", candidates);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-FILTER-10: filterCandidates с короткими запросами")
    void filterCandidates_shortQuery() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);
        when(properties.getShortQueryMaxLength()).thenReturn(4);

        FileMetadataDto match = new FileMetadataDto(
                1L, "Cat Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), "Cats playing"
        );

        List<FileMetadataDto> result = service.filterCandidates("cat", List.of(match));

        assertThat(result).contains(match);
    }

    private List<FileMetadataDto> createTestCandidates(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new FileMetadataDto(
                        (long) i,
                        "Video " + i,
                        UUID.randomUUID(),
                        "video/mp4",
                        1000L,
                        (long) i,
                        "uploader" + i,
                        null,
                        10L,
                        null,
                        720,
                        List.of(720),
                        "Description \" + i"
                ))
                .toList();
    }
}
