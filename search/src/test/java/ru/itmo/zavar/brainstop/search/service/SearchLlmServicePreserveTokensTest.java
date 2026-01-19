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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServicePreserveTokensTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;
    private Method extractPreserveTokensMethod;
    private Method ensureTokensPresentMethod;
    private Method removeTransliterationsMethod;
    private Method buildPreserveTokensPromptMethod;
    private Method containsAllTokensMethod;
    private Method containsDigitMethod;
    private Method containsUppercaseMethod;
    private Method isShortLatinTokenMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.getCacheTtl()).thenReturn(Duration.ofMinutes(5));
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();

        extractPreserveTokensMethod = SearchLlmService.class.getDeclaredMethod("extractPreserveTokens", String.class);
        extractPreserveTokensMethod.setAccessible(true);

        ensureTokensPresentMethod = SearchLlmService.class.getDeclaredMethod("ensureTokensPresent", String.class, List.class);
        ensureTokensPresentMethod.setAccessible(true);

        removeTransliterationsMethod = SearchLlmService.class.getDeclaredMethod("removeTransliterations", String.class, List.class);
        removeTransliterationsMethod.setAccessible(true);

        buildPreserveTokensPromptMethod = SearchLlmService.class.getDeclaredMethod("buildPreserveTokensPrompt", String.class);
        buildPreserveTokensPromptMethod.setAccessible(true);

        containsAllTokensMethod = SearchLlmService.class.getDeclaredMethod("containsAllTokens", String.class, List.class);
        containsAllTokensMethod.setAccessible(true);

        containsDigitMethod = SearchLlmService.class.getDeclaredMethod("containsDigit", String.class);
        containsDigitMethod.setAccessible(true);

        containsUppercaseMethod = SearchLlmService.class.getDeclaredMethod("containsUppercase", String.class);
        containsUppercaseMethod.setAccessible(true);

        isShortLatinTokenMethod = SearchLlmService.class.getDeclaredMethod("isShortLatinToken", String.class);
        isShortLatinTokenMethod.setAccessible(true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-01: extractPreserveTokens находит числа")
    void extractPreserveTokens_numbers() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractPreserveTokensMethod.invoke(service, "iPhone 15 Pro");

        assertThat(result).contains("15");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-02: extractPreserveTokens находит бренды")
    void extractPreserveTokens_brands() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractPreserveTokensMethod.invoke(service, "BMW X5");

        assertThat(result).contains("BMW", "X5");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-03: extractPreserveTokens пропускает стоп-слова")
    void extractPreserveTokens_skipsStopWords() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractPreserveTokensMethod.invoke(service, "the cat and dog");

        assertThat(result).doesNotContain("the", "and");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-04: extractPreserveTokens с пустой строкой")
    void extractPreserveTokens_empty() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractPreserveTokensMethod.invoke(service, "");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-05: ensureTokensPresent добавляет токены")
    void ensureTokensPresent_adds() throws Exception {
        String result = (String) ensureTokensPresentMethod.invoke(service, "test video", List.of("BMW"));

        assertThat(result).contains("BMW");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-06: ensureTokensPresent не дублирует")
    void ensureTokensPresent_noDuplicates() throws Exception {
        String result = (String) ensureTokensPresentMethod.invoke(service, "test BMW", List.of("BMW"));

        assertThat(result).isEqualTo("test BMW");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-07: removeTransliterations удаляет транслиты")
    void removeTransliterations_removes() throws Exception {
        String result = (String) removeTransliterationsMethod.invoke(service, "test BMW БМВ", List.of("BMW"));

        assertThat(result).doesNotContain("БМВ");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-08: buildPreserveTokensPrompt формирует промпт")
    void buildPreserveTokensPrompt() throws Exception {
        String result = (String) buildPreserveTokensPromptMethod.invoke(service, "BMW X5 test");

        assertThat(result).contains("BMW", "X5");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-09: containsAllTokens проверяет все токены")
    void containsAllTokens_all() throws Exception {
        Boolean result = (Boolean) containsAllTokensMethod.invoke(service, "BMW X5 test", List.of("BMW", "X5"));

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-10: containsAllTokens false если не все")
    void containsAllTokens_notAll() throws Exception {
        Boolean result = (Boolean) containsAllTokensMethod.invoke(service, "BMW test", List.of("BMW", "X5"));

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-11: containsDigit находит цифры")
    void containsDigit_true() throws Exception {
        Boolean result = (Boolean) containsDigitMethod.invoke(service, "test123");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-12: containsDigit false без цифр")
    void containsDigit_false() throws Exception {
        Boolean result = (Boolean) containsDigitMethod.invoke(service, "test");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-13: containsUppercase находит заглавные")
    void containsUppercase_true() throws Exception {
        Boolean result = (Boolean) containsUppercaseMethod.invoke(service, "Test");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-14: containsUppercase false без заглавных")
    void containsUppercase_false() throws Exception {
        Boolean result = (Boolean) containsUppercaseMethod.invoke(service, "test");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-15: isShortLatinToken для акронимов")
    void isShortLatinToken_acronym() throws Exception {
        Boolean result = (Boolean) isShortLatinTokenMethod.invoke(service, "USA");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-16: isShortLatinToken false для длинных")
    void isShortLatinToken_long() throws Exception {
        Boolean result = (Boolean) isShortLatinTokenMethod.invoke(service, "testing");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-17: extractPreserveTokens с акронимами")
    void extractPreserveTokens_acronyms() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractPreserveTokensMethod.invoke(service, "USA FBI CIA");

        assertThat(result).contains("USA", "FBI", "CIA");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-18: extractPreserveTokens с моделями")
    void extractPreserveTokens_models() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractPreserveTokensMethod.invoke(service, "iPhone 15 Pro Max");

        assertThat(result).contains("iPhone", "15");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-19: ensureTokensPresent с несколькими токенами")
    void ensureTokensPresent_multiple() throws Exception {
        String result = (String) ensureTokensPresentMethod.invoke(service, "test", List.of("BMW", "X5", "2024"));

        assertThat(result).contains("BMW", "X5", "2024");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-PRESERVE-20: removeTransliterations сохраняет оригиналы")
    void removeTransliterations_preservesOriginals() throws Exception {
        String result = (String) removeTransliterationsMethod.invoke(service, "BMW БМВ X5", List.of("BMW"));

        assertThat(result).contains("BMW");
    }
}
