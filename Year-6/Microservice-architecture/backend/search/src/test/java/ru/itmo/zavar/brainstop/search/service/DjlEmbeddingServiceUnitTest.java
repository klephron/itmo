package ru.itmo.zavar.brainstop.search.service;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
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
import ru.itmo.zavar.brainstop.search.embedding.DjlEmbeddingService;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class DjlEmbeddingServiceUnitTest {

    @Mock
    private ZooModel<String, float[]> model;

    @Mock
    private Predictor<String, float[]> predictor;

    private DjlEmbeddingService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new DjlEmbeddingService();
        Field modelField = DjlEmbeddingService.class.getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(service, model);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-DJL-MOCK-01: Embed с моделью возвращает вектор")
    void embed_withModel_returnsVector() throws TranslateException {
        float[] expected = new float[]{0.1f, 0.2f, 0.3f};
        when(model.newPredictor()).thenReturn(predictor);
        when(predictor.predict(anyString())).thenReturn(expected);

        float[] result = service.embed("test");

        float norm = (float) Math.sqrt(0.1f * 0.1f + 0.2f * 0.2f + 0.3f * 0.3f);
        float[] expectedNormalized = new float[]{expected[0] / norm, expected[1] / norm, expected[2] / norm};
        assertThat(result[0]).isCloseTo(expectedNormalized[0], org.assertj.core.data.Offset.offset(1e-6f));
        assertThat(result[1]).isCloseTo(expectedNormalized[1], org.assertj.core.data.Offset.offset(1e-6f));
        assertThat(result[2]).isCloseTo(expectedNormalized[2], org.assertj.core.data.Offset.offset(1e-6f));
        verify(predictor).predict("test");
        verify(predictor).close();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DJL-MOCK-02: TranslateException fallback на hashing")
    void embed_translateException_fallsBack() throws TranslateException {
        when(model.newPredictor()).thenReturn(predictor);
        when(predictor.predict(anyString())).thenThrow(new TranslateException("test error"));

        float[] result = service.embed("test");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1024);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DJL-MOCK-03: Null модель использует fallback")
    void embed_nullModel_usesFallback() throws Exception {
        Field modelField = DjlEmbeddingService.class.getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(service, null);

        float[] result = service.embed("test");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1024);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DJL-MOCK-04: Пустая строка возвращает нулевой вектор")
    void embed_emptyString_returnsZeroVector() {
        float[] result = service.embed("");

        assertThat(result).hasSize(1024);
        assertThat(result).containsOnly(0.0f);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DJL-MOCK-05: Null строка возвращает нулевой вектор")
    void embed_nullString_returnsZeroVector() {
        float[] result = service.embed(null);

        assertThat(result).hasSize(1024);
        assertThat(result).containsOnly(0.0f);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DJL-MOCK-06: Пробельная строка возвращает нулевой вектор")
    void embed_blankString_returnsZeroVector() {
        float[] result = service.embed("   \t\n");

        assertThat(result).hasSize(1024);
        assertThat(result).containsOnly(0.0f);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-DJL-MOCK-07: Predictor корректно закрывается")
    void embed_predictorClosedProperly() throws TranslateException {
        when(model.newPredictor()).thenReturn(predictor);
        when(predictor.predict(anyString())).thenReturn(new float[]{0.1f});

        service.embed("test");

        verify(predictor).close();
    }
}
