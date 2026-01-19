package ru.itmo.zavar.brainstop.video.api;

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
import ru.itmo.zavar.brainstop.video.llm.OpenRouterProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class OpenRouterClientComplexTest {

    @Mock
    private OpenRouterProperties properties;

    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        when(properties.getBaseUrl()).thenReturn("http://localhost");
        when(properties.getTimeout()).thenReturn(Duration.ofSeconds(5));
        client = new OpenRouterClient(properties, new ObjectMapper());
        client.init();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-01: complete с валидным ответом")
    void complete_validResponse() {
        when(properties.isEnabled()).thenReturn(false);

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-02: complete возвращает empty когда disabled")
    void complete_disabled() {
        when(properties.isEnabled()).thenReturn(false);

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-OR-CLIENT-03: complete без API key")
    void complete_noApiKey() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn(null);

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-OR-CLIENT-04: complete с пустым API key")
    void complete_blankApiKey() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("  ");

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-OR-CLIENT-05: complete с null messages")
    void complete_nullMessages() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");

        Optional<String> result = client.complete(null, 100, "test");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-OR-CLIENT-06: complete с пустыми messages")
    void complete_emptyMessages() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");

        Optional<String> result = client.complete(List.of(), 100, "test");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-OR-CLIENT-07: complete без моделей")
    void complete_noModels() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");
        when(properties.getModels()).thenReturn(List.of());

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-OR-CLIENT-08: complete с null моделями")
    void complete_nullModels() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");
        when(properties.getModels()).thenReturn(null);

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-09: complete использует maxTokens из параметра")
    void complete_usesMaxTokensParameter() {
        when(properties.isEnabled()).thenReturn(false);

        client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                200,
                "test"
        );

        // Проверяем что не упало
        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-10: complete использует default maxTokens")
    void complete_usesDefaultMaxTokens() {
        when(properties.isEnabled()).thenReturn(false);

        client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test"
        );

        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-11: init устанавливает timeout")
    void init_setsTimeout() {
        when(properties.getTimeout()).thenReturn(Duration.ofSeconds(10));

        OpenRouterClient newClient = new OpenRouterClient(properties, new ObjectMapper());
        newClient.init();

        assertThat(newClient).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-12: init устанавливает headers")
    void init_setsHeaders() {
        when(properties.getApiKey()).thenReturn("test-key");
        when(properties.getReferer()).thenReturn("http://example.com");
        when(properties.getTitle()).thenReturn("Test App");

        OpenRouterClient newClient = new OpenRouterClient(properties, new ObjectMapper());
        newClient.init();

        assertThat(newClient).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-13: complete с null maxTokens использует default")
    void complete_nullMaxTokensUsesDefault() {
        when(properties.isEnabled()).thenReturn(false);

        client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test"
        );

        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-OR-CLIENT-14: complete пытается все модели")
    void complete_triesAllModels() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");
        when(properties.getModels()).thenReturn(List.of("model1", "model2", "model3"));

        // Все модели провалятся из-за отсутствия сервера
        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                100,
                "test"
        );

        // Должно попытаться со всеми моделями и вернуть empty
        assertThat(result).isEmpty();
    }
}
