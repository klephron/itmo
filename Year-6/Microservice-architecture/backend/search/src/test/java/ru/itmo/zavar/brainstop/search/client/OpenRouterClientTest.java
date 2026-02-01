package ru.itmo.zavar.brainstop.search.client;

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
import ru.itmo.zavar.brainstop.search.llm.OpenRouterProperties;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class OpenRouterClientTest {

    @Mock
    private OpenRouterProperties properties;

    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        client = new OpenRouterClient(properties, new ObjectMapper());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ORCLIENT-01: complete возвращает empty когда disabled")
    void complete_disabled() {
        when(properties.isEnabled()).thenReturn(false);

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ORCLIENT-02: complete возвращает empty при отсутствии API key")
    void complete_noApiKey() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn(null);

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ORCLIENT-03: complete возвращает empty при пустых messages")
    void complete_emptyMessages() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");

        Optional<String> result = client.complete(List.of(), null, "test");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ORCLIENT-04: complete возвращает empty при null messages")
    void complete_nullMessages() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");

        Optional<String> result = client.complete(null, null, "test");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ORCLIENT-05: complete возвращает empty при пустом списке моделей")
    void complete_noModels() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("key");
        when(properties.getModels()).thenReturn(List.of());

        Optional<String> result = client.complete(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ORCLIENT-06: stream возвращает empty когда disabled")
    void stream_disabled() {
        when(properties.isEnabled()).thenReturn(false);

        Optional<String> result = client.stream(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test",
                null
        );

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ORCLIENT-07: stream возвращает empty при отсутствии API key")
    void stream_noApiKey() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getApiKey()).thenReturn("");

        Optional<String> result = client.stream(
                List.of(new OpenRouterClient.OpenRouterMessage("user", "test")),
                null,
                "test",
                null
        );

        assertThat(result).isEmpty();
    }
}
