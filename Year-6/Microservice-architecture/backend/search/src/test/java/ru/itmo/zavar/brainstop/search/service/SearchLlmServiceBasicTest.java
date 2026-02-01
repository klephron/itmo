package ru.itmo.zavar.brainstop.search.service;

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
import ru.itmo.zavar.brainstop.search.llm.OpenRouterClient;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmProperties;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceBasicTest {

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
    @DisplayName("BE-UNIT-LLM-SVC-01: minQueryLength возвращает размер из properties")
    void minQueryLength() {
        when(properties.getMinQueryLength()).thenReturn(5);

        int result = service.minQueryLength();

        assertThat(result).isEqualTo(5);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LLM-SVC-02: minQueryLength с отрицательным значением возвращает 0")
    void minQueryLength_negative() {
        when(properties.getMinQueryLength()).thenReturn(-1);

        int result = service.minQueryLength();

        assertThat(result).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-03: isLlmEnabled возвращает значение из properties")
    void isLlmEnabled() {
        when(properties.isEnabled()).thenReturn(true);

        boolean result = service.isLlmEnabled();

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-04: isRewriteEnabled зависит от enabled и rewriteEnabled")
    void isRewriteEnabled() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isRewriteEnabled()).thenReturn(true);

        boolean result = service.isRewriteEnabled();

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-05: isExpansionEnabled зависит от enabled и expansionEnabled")
    void isExpansionEnabled() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);

        boolean result = service.isExpansionEnabled();

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-06: isFilterEnabled зависит от enabled и filterEnabled")
    void isFilterEnabled() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isFilterEnabled()).thenReturn(true);

        boolean result = service.isFilterEnabled();

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-07: isRagEnabled возвращает значение из properties")
    void isRagEnabled() {
        when(properties.isRagEnabled()).thenReturn(true);

        boolean result = service.isRagEnabled();

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-08: ragLimit возвращает значение из properties")
    void ragLimit() {
        when(properties.getRagMaxItems()).thenReturn(10);

        int result = service.ragLimit();

        assertThat(result).isEqualTo(10);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LLM-SVC-09: ragLimit с отрицательным значением возвращает 0")
    void ragLimit_negative() {
        when(properties.getRagMaxItems()).thenReturn(-5);

        int result = service.ragLimit();

        assertThat(result).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-10: poolSize вычисляется корректно")
    void poolSize() {
        when(properties.getPoolMultiplier()).thenReturn(2);
        when(properties.getMaxPoolSize()).thenReturn(100);

        int result = service.poolSize(0, 10);

        assertThat(result).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-11: poolSize ограничивается maxPoolSize")
    void poolSize_limitedByMax() {
        when(properties.getPoolMultiplier()).thenReturn(10);
        when(properties.getMaxPoolSize()).thenReturn(50);

        int result = service.poolSize(10, 10);

        assertThat(result).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-12: suggestPoolSize вычисляется корректно")
    void suggestPoolSize() {
        when(properties.getSuggestMultiplier()).thenReturn(2);
        when(properties.getMaxPoolSize()).thenReturn(100);

        int result = service.suggestPoolSize(10);

        assertThat(result).isEqualTo(20);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-13: buildRagContext возвращает пустую строку когда RAG disabled")
    void buildRagContext_disabled() {
        when(properties.isRagEnabled()).thenReturn(false);

        String result = service.buildRagContext(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-14: buildRagContext с пустым списком")
    void buildRagContext_emptyList() {
        when(properties.isRagEnabled()).thenReturn(true);

        String result = service.buildRagContext(new ArrayList<>());

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-15: buildRagContext с null списком")
    void buildRagContext_nullList() {
        when(properties.isRagEnabled()).thenReturn(true);

        String result = service.buildRagContext(null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-17: shouldUseLlmVariants с коротким запросом")
    void shouldUseLlmVariants_shortQuery() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMinQueryLength()).thenReturn(5);

        boolean result = service.shouldUseLlmVariants("abc");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-18: shouldUseLlmVariants с длинным запросом")
    void shouldUseLlmVariants_longQuery() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMinQueryLength()).thenReturn(3);

        boolean result = service.shouldUseLlmVariants("test query");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-19: buildQueryVariants с пустым запросом")
    void buildQueryVariants_emptyQuery() {
        List<String> result = service.buildQueryVariants("");

        assertThat(result).containsExactly("");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-SVC-20: buildQueryVariants с LLM disabled")
    void buildQueryVariants_llmDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        List<String> result = service.buildQueryVariants("test query");

        assertThat(result).containsExactly("test query");
    }
}
