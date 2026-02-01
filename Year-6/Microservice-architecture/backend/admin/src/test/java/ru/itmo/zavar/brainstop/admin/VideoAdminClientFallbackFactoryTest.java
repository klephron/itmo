package ru.itmo.zavar.brainstop.admin.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"NFR-3.1", "NFR-3.2"})
@Layer(TestLayer.BE_UNIT)
class VideoAdminClientFallbackFactoryTest {

    private VideoAdminClientFallbackFactory factory;
    private VideoAdminClient fallbackClient;

    @BeforeEach
    void setUp() {
        factory = new VideoAdminClientFallbackFactory();
        fallbackClient = factory.create(new RuntimeException("Service unavailable"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-06: Fallback возвращает unavailable для регенерации")
    void fallback_regenerateThumbnails_returnsUnavailable() {
        var result = fallbackClient.regenerateThumbnails(true);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("unavailable");
        assertThat(result.get("message")).isEqualTo("video service not available");
    }
}
