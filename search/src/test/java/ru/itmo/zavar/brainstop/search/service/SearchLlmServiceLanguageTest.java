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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceLanguageTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;
    private Method containsCyrillicMethod;
    private Method containsLatinMethod;
    private Method transliterateToLatinMethod;
    private Method transliterateToCyrillicMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.getCacheTtl()).thenReturn(Duration.ofMinutes(5));
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();

        containsCyrillicMethod = SearchLlmService.class.getDeclaredMethod("containsCyrillic", String.class);
        containsCyrillicMethod.setAccessible(true);

        containsLatinMethod = SearchLlmService.class.getDeclaredMethod("containsLatin", String.class);
        containsLatinMethod.setAccessible(true);

        transliterateToLatinMethod = SearchLlmService.class.getDeclaredMethod("transliterateToLatin", String.class);
        transliterateToLatinMethod.setAccessible(true);

        transliterateToCyrillicMethod = SearchLlmService.class.getDeclaredMethod("transliterateToCyrillic", String.class);
        transliterateToCyrillicMethod.setAccessible(true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-01: containsCyrillic определяет кириллицу")
    void containsCyrillic_detectsCyrillic() throws Exception {
        Boolean result = (Boolean) containsCyrillicMethod.invoke(service, "тест");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-02: containsCyrillic не находит в латинице")
    void containsCyrillic_notInLatin() throws Exception {
        Boolean result = (Boolean) containsCyrillicMethod.invoke(service, "test");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-03: containsLatin определяет латиницу")
    void containsLatin_detectsLatin() throws Exception {
        Boolean result = (Boolean) containsLatinMethod.invoke(service, "test");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-04: containsLatin не находит в кириллице")
    void containsLatin_notInCyrillic() throws Exception {
        Boolean result = (Boolean) containsLatinMethod.invoke(service, "тест");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-05: transliterateToLatin конвертирует")
    void transliterateToLatin_converts() throws Exception {
        String result = (String) transliterateToLatinMethod.invoke(service, "тест");

        assertThat(result).isEqualTo("test");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-06: transliterateToLatin с ё")
    void transliterateToLatin_yo() throws Exception {
        String result = (String) transliterateToLatinMethod.invoke(service, "ёлка");

        assertThat(result).contains("yo");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-07: transliterateToCyrillic конвертирует")
    void transliterateToCyrillic_converts() throws Exception {
        String result = (String) transliterateToCyrillicMethod.invoke(service, "test");

        assertThat(result).containsPattern(".*[\u0430-\u044f].*");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-08: transliterateToCyrillic с sh")
    void transliterateToCyrillic_sh() throws Exception {
        String result = (String) transliterateToCyrillicMethod.invoke(service, "shcha");

        assertThat(result).contains("щ");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-09: transliterateToLatin с пробелами")
    void transliterateToLatin_whitespace() throws Exception {
        String result = (String) transliterateToLatinMethod.invoke(service, "тест запрос");

        assertThat(result).contains(" ");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-LANG-10: transliterateToCyrillic сохраняет пробелы")
    void transliterateToCyrillic_preservesWhitespace() throws Exception {
        String result = (String) transliterateToCyrillicMethod.invoke(service, "test query");

        assertThat(result).contains(" ");
    }
}
