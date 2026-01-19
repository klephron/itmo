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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceRagTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;

    @BeforeEach
    void setUp() {
        when(properties.getCacheTtl()).thenReturn(Duration.ofMinutes(5));
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-01: buildRagContext форматирует контекст")
    void buildRagContext_formats() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(10);
        when(properties.getRagMaxChars()).thenReturn(1000);
        when(properties.getRagMaxFieldLength()).thenReturn(100);

        FileMetadataDto dto = new FileMetadataDto(
                1L, "Test Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "TestUser", null, 10L, null, 720, List.of(720), "A test description"
        );

        String result = service.buildRagContext(List.of(dto));

        assertThat(result).contains("uuid=");
        assertThat(result).contains("name=Test Video");
        assertThat(result).contains("desc=A test description");
        assertThat(result).contains("uploader=TestUser");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-02: buildRagContext ограничивает количество")
    void buildRagContext_limitsCount() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(2);
        when(properties.getRagMaxChars()).thenReturn(10000);
        when(properties.getRagMaxFieldLength()).thenReturn(100);

        List<FileMetadataDto> candidates = createCandidates(5);

        String result = service.buildRagContext(candidates);

        long lineCount = result.lines().count();
        assertThat(lineCount).isLessThanOrEqualTo(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-03: buildRagContext ограничивает символы")
    void buildRagContext_limitsChars() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(100);
        when(properties.getRagMaxChars()).thenReturn(100);
        when(properties.getRagMaxFieldLength()).thenReturn(50);

        List<FileMetadataDto> candidates = createCandidates(10);

        String result = service.buildRagContext(candidates);

        assertThat(result.length()).isLessThanOrEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-04: buildRagContext обрезает длинные поля")
    void buildRagContext_truncatesFields() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(10);
        when(properties.getRagMaxChars()).thenReturn(10000);
        when(properties.getRagMaxFieldLength()).thenReturn(20);

        String longDesc = "a".repeat(100);
        FileMetadataDto dto = new FileMetadataDto(
                1L, "Test", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), longDesc
        );

        String result = service.buildRagContext(List.of(dto));

        assertThat(result).doesNotContain(longDesc);
        assertThat(result.length()).isLessThan(longDesc.length() + 50);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-05: buildRagContext пропускает null поля")
    void buildRagContext_skipsNullFields() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(10);
        when(properties.getRagMaxChars()).thenReturn(1000);
        when(properties.getRagMaxFieldLength()).thenReturn(100);

        FileMetadataDto dto = new FileMetadataDto(
                1L, null, UUID.randomUUID(), "video/mp4", 1000L,
                1L, null, null, 10L, null, 720, List.of(720), ""
        );

        String result = service.buildRagContext(List.of(dto));

        assertThat(result).contains("uuid=");
        assertThat(result).doesNotContain("name=");
        assertThat(result).doesNotContain("desc=");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-06: buildRagContext с пустыми строками")
    void buildRagContext_emptyStrings() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(10);
        when(properties.getRagMaxChars()).thenReturn(1000);
        when(properties.getRagMaxFieldLength()).thenReturn(100);

        FileMetadataDto dto = new FileMetadataDto(
                1L, "", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "", null, 10L, null, 720, List.of(720), ""
        );

        String result = service.buildRagContext(List.of(dto));

        assertThat(result).contains("uuid=");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-08: buildRagContext очищает пробелы")
    void buildRagContext_cleansWhitespace() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(10);
        when(properties.getRagMaxChars()).thenReturn(1000);
        when(properties.getRagMaxFieldLength()).thenReturn(100);

        FileMetadataDto dto = new FileMetadataDto(
                1L, "Test  \n  Video", UUID.randomUUID(), "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), "Desc  \t  Text"
        );

        String result = service.buildRagContext(List.of(dto));

        assertThat(result).doesNotContain("\n");
        assertThat(result).doesNotContain("\t");
        assertThat(result).doesNotContain("  ");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-09: buildCandidateContext вызывает buildRagContext")
    void buildCandidateContext() {
        when(properties.getFilterMaxItems()).thenReturn(10);
        when(properties.getFilterMaxChars()).thenReturn(1000);
        when(properties.getFilterMaxFieldLength()).thenReturn(100);

        List<FileMetadataDto> candidates = createCandidates(3);

        String result = service.buildCandidateContext(candidates);

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-RAG-10: buildRagContext с нулевым maxItems")
    void buildRagContext_zeroMaxItems() {
        when(properties.isRagEnabled()).thenReturn(true);
        when(properties.getRagMaxItems()).thenReturn(0);

        String result = service.buildRagContext(createCandidates(5));

        assertThat(result).isEmpty();
    }

    private List<FileMetadataDto> createCandidates(int count) {
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
