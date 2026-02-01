package ru.itmo.zavar.brainstop.search.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class EmbeddingInternalControllerTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private EmbeddingInternalController controller;

    @BeforeEach
    void setUp() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EMB-CTL-01: Возвращает вектор для текста")
    void embed_returnsVector() {
        EmbeddingResponse response = controller.embed("test query");

        assertThat(response.vector()).hasSize(3);
        assertThat(response.vector()).containsExactly(0.1f, 0.2f, 0.3f);
        verify(embeddingService).embed("test query");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-EMB-CTL-02: Обрабатывает null текст")
    void embed_nullText() {
        EmbeddingResponse response = controller.embed(null);

        assertThat(response.vector()).hasSize(3);
        verify(embeddingService).embed("");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EMB-CTL-03: Обрабатывает пустую строку")
    void embed_emptyString() {
        EmbeddingResponse response = controller.embed("");

        assertThat(response.vector()).hasSize(3);
        verify(embeddingService).embed("");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EMB-CTL-04: Обрабатывает длинный текст")
    void embed_longText() {
        String longText = "This is a very long text with many words";

        EmbeddingResponse response = controller.embed(longText);

        assertThat(response.vector()).isNotNull();
        verify(embeddingService).embed(longText);
    }
}
