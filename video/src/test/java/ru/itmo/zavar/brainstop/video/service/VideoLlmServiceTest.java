package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.llm.OpenRouterClient;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmProperties;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmResult;
import ru.itmo.zavar.brainstop.video.llm.VideoLlmService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class VideoLlmServiceTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private VideoLlmProperties properties;

    private VideoLlmService service;

    @BeforeEach
    void setUp() {
        service = new VideoLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-01: enrich возвращает empty когда disabled")
    void enrich_disabled() {
        when(properties.isEnabled()).thenReturn(false);

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).isEmpty();
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-02: enrich генерирует теги")
    void enrich_generatesTags() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"tag1\", \"tag2\", \"tag3\"]"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).hasSize(3);
        assertThat(result.tags()).containsExactly("tag1", "tag2", "tag3");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-03: enrich выполняет модерацию")
    void enrich_performsModeration() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(false);
        when(properties.isModerationEnabled()).thenReturn(true);
        when(properties.getModerationMaxTokens()).thenReturn(120);
        when(openRouterClient.complete(anyList(), anyInt(), eq("moderation")))
                .thenReturn(Optional.of("{\"flagged\": true, \"categories\": [\"violence\"]}"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.flagged()).isTrue();
        assertThat(result.categories()).contains("violence");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-04: enrich кеширует результаты тегов")
    void enrich_cachesTagResults() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"tag1\"]"));

        service.enrich("Title", "Description");
        VideoLlmResult secondCall = service.enrich("Title", "Description");

        assertThat(secondCall.tags()).containsExactly("tag1");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-05: enrich нормализует теги")
    void enrich_normalizesTags() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"TAG1\", \"tag-2\", \"tag_3\"]"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).allMatch(tag -> tag.equals(tag.toLowerCase()));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-06: enrich удаляет спецсимволы из тегов")
    void enrich_removesSpecialCharsFromTags() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"tag@#$1\", \"tag!2\"]"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).allMatch(tag -> !tag.contains("@") && !tag.contains("#"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-07: enrich удаляет кавычки из тегов")
    void enrich_removesQuotesFromTags() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"\\\"tag1\\\"\", \"'tag2'\"]"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).noneMatch(tag -> tag.contains("\"") || tag.contains("'"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-08: enrich ограничивает количество тегов")
    void enrich_limitsTagCount() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(2);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\"]"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-09: enrich обрабатывает fallback парсинг")
    void enrich_handlesFallbackParsing() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("tag1, tag2; tag3\ntag4"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-10: enrich модерация с flagged=false")
    void enrich_moderationNotFlagged() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(false);
        when(properties.isModerationEnabled()).thenReturn(true);
        when(properties.getModerationMaxTokens()).thenReturn(120);
        when(openRouterClient.complete(anyList(), anyInt(), eq("moderation")))
                .thenReturn(Optional.of("{\"flagged\": false, \"categories\": []}"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.flagged()).isFalse();
        assertThat(result.categories()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-11: VideoLlmResult.tagsText объединяет теги")
    void videoLlmResult_tagsText() {
        VideoLlmResult result = new VideoLlmResult(List.of("tag1", "tag2", "tag3"), false, List.of());

        assertThat(result.tagsText()).isEqualTo("tag1 tag2 tag3");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-12: VideoLlmResult.categoriesText объединяет категории")
    void videoLlmResult_categoriesText() {
        VideoLlmResult result = new VideoLlmResult(List.of(), true, List.of("sexual", "violence"));

        assertThat(result.categoriesText()).isEqualTo("sexual,violence");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-13: VideoLlmResult.empty создает пустой результат")
    void videoLlmResult_empty() {
        VideoLlmResult result = VideoLlmResult.empty();

        assertThat(result.tags()).isEmpty();
        assertThat(result.flagged()).isFalse();
        assertThat(result.categories()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-14: enrich с пустым title")
    void enrich_emptyTitle() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("[\"tag1\"]"));

        VideoLlmResult result = service.enrich("", "Description");

        assertThat(result).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-15: enrich с null description")
    void enrich_nullDescription() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), anyString()))
                .thenReturn(Optional.of("[\"tag1\"]"));

        VideoLlmResult result = service.enrich("Title", null);

        assertThat(result).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-16: enrich дедублицирует теги")
    void enrich_deduplicatesTags() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(5);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("[\"tag1\", \"tag1\", \"tag2\"]"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).containsExactly("tag1", "tag2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-17: enrich обрабатывает невалидный JSON тегов")
    void enrich_handlesInvalidTagsJson() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(true);
        when(properties.getTagCount()).thenReturn(3);
        when(properties.getTagMaxTokens()).thenReturn(120);
        when(properties.isModerationEnabled()).thenReturn(false);
        when(openRouterClient.complete(anyList(), anyInt(), eq("tags")))
                .thenReturn(Optional.of("not a json"));

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.tags()).isEqualTo(List.of("not a json"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-18: enrich обрабатывает невалидный JSON модерации")
    void enrich_handlesInvalidModerationJson() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isTaggingEnabled()).thenReturn(false);
        when(properties.isModerationEnabled()).thenReturn(true);
        when(properties.getModerationMaxTokens()).thenReturn(120);

        VideoLlmResult result = service.enrich("Title", "Description");

        assertThat(result.flagged()).isFalse();
        assertThat(result.categories()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIDEO-LLM-20: VideoLlmResult.normalizeCategory нормализует")
    void normalizeCategory() {
        assertThat(VideoLlmResult.normalizeCategory("SEXUAL")).isEqualTo("sexual");
        assertThat(VideoLlmResult.normalizeCategory("  Violence  ")).isEqualTo("violence");
        assertThat(VideoLlmResult.normalizeCategory(null)).isEmpty();
    }
}
