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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceUtilsTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;
    private Method joinPartsMethod;
    private Method cleanTextMethod;
    private Method contextKeyMethod;
    private Method candidateKeyMethod;
    private Method limitWordsMethod;
    private Method splitQueryTokensMethod;
    private Method addTokenVariantsMethod;
    private Method isSimpleQueryMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.getCacheTtl()).thenReturn(Duration.ofMinutes(5));
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();

        joinPartsMethod = SearchLlmService.class.getDeclaredMethod("joinParts", String[].class);
        joinPartsMethod.setAccessible(true);

        cleanTextMethod = SearchLlmService.class.getDeclaredMethod("cleanText", String.class, int.class);
        cleanTextMethod.setAccessible(true);

        contextKeyMethod = SearchLlmService.class.getDeclaredMethod("contextKey", String.class);
        contextKeyMethod.setAccessible(true);

        candidateKeyMethod = SearchLlmService.class.getDeclaredMethod("candidateKey", FileMetadataDto.class);
        candidateKeyMethod.setAccessible(true);

        limitWordsMethod = SearchLlmService.class.getDeclaredMethod("limitWords", String.class, int.class);
        limitWordsMethod.setAccessible(true);

        splitQueryTokensMethod = SearchLlmService.class.getDeclaredMethod("splitQueryTokens", String.class);
        splitQueryTokensMethod.setAccessible(true);

        isSimpleQueryMethod = SearchLlmService.class.getDeclaredMethod("isSimpleQuery", String.class);
        isSimpleQueryMethod.setAccessible(true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-01: joinParts объединяет части")
    void joinParts_joins() throws Exception {
        String result = (String) joinPartsMethod.invoke(service, (Object) new String[]{"part1", "part2", "part3"});

        assertThat(result).contains("part1", "part2", "part3");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-02: joinParts пропускает null")
    void joinParts_skipsNull() throws Exception {
        String result = (String) joinPartsMethod.invoke(service, (Object) new String[]{"part1", null, "part2"});

        assertThat(result).contains("part1", "part2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-03: cleanText удаляет пробелы")
    void cleanText_removesWhitespace() throws Exception {
        String result = (String) cleanTextMethod.invoke(service, "test  \n  value", 100);

        assertThat(result).isEqualTo("test value");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-04: cleanText обрезает по длине")
    void cleanText_truncates() throws Exception {
        String result = (String) cleanTextMethod.invoke(service, "very long text here", 10);

        assertThat(result).hasSize(10);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-05: contextKey создает хеш")
    void contextKey_hash() throws Exception {
        String result = (String) contextKeyMethod.invoke(service, "test context");

        assertThat(result).isNotEmpty();
        assertThat(result).isNotEqualTo("test context");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-06: contextKey для null возвращает none")
    void contextKey_null() throws Exception {
        String result = (String) contextKeyMethod.invoke(service, (String) null);

        assertThat(result).isEqualTo("none");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-07: candidateKey возвращает UUID")
    void candidateKey_uuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(
                1L, "Test", uuid, "video/mp4", 1000L,
                1L, "User", null, 10L, null, 720, List.of(720), "Desc"
        );

        String result = (String) candidateKeyMethod.invoke(service, dto);

        assertThat(result).contains(uuid.toString().toLowerCase());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-08: candidateKey для null возвращает null")
    void candidateKey_null() throws Exception {
        String result = (String) candidateKeyMethod.invoke(service, (FileMetadataDto) null);

        assertThat(result).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-09: limitWords ограничивает слова")
    void limitWords_limits() throws Exception {
        String result = (String) limitWordsMethod.invoke(service, "one two three four five", 3);

        assertThat(result).isEqualTo("one two three");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-10: limitWords не обрезает короткие")
    void limitWords_short() throws Exception {
        String result = (String) limitWordsMethod.invoke(service, "one two", 5);

        assertThat(result).isEqualTo("one two");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-11: splitQueryTokens разбивает")
    void splitQueryTokens_splits() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitQueryTokensMethod.invoke(service, "one two three");

        assertThat(result).containsExactly("one", "two", "three");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-12: splitQueryTokens удаляет спецсимволы")
    void splitQueryTokens_removesSpecial() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitQueryTokensMethod.invoke(service, "one@two#three");

        assertThat(result).hasSize(3);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-13: isSimpleQuery для коротких запросов")
    void isSimpleQuery_short() throws Exception {
        when(properties.getShortQueryMaxLength()).thenReturn(4);

        Boolean result = (Boolean) isSimpleQueryMethod.invoke(service, "cat");

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-14: isSimpleQuery false для длинных")
    void isSimpleQuery_long() throws Exception {
        Boolean result = (Boolean) isSimpleQueryMethod.invoke(service, "test query");

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-15: cleanText с null возвращает пустую строку")
    void cleanText_null() throws Exception {
        String result = (String) cleanTextMethod.invoke(service, null, 100);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-16: joinParts пропускает пустые строки")
    void joinParts_skipsEmpty() throws Exception {
        String result = (String) joinPartsMethod.invoke(service, (Object) new String[]{"part1", "", "part2"});

        assertThat(result).contains("part1", "part2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-17: contextKey одинаковый для одинаковых")
    void contextKey_same() throws Exception {
        String result1 = (String) contextKeyMethod.invoke(service, "test");
        String result2 = (String) contextKeyMethod.invoke(service, "test");

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-18: limitWords с нулевым лимитом")
    void limitWords_zero() throws Exception {
        String result = (String) limitWordsMethod.invoke(service, "one two three", 0);

        assertThat(result).isEqualTo("one two three");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-19: splitQueryTokens пустой для null")
    void splitQueryTokens_null() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitQueryTokensMethod.invoke(service, (String) null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-UTILS-20: isSimpleQuery true для пустых")
    void isSimpleQuery_empty() throws Exception {
        Boolean result = (Boolean) isSimpleQueryMethod.invoke(service, "");

        assertThat(result).isTrue();
    }
}
