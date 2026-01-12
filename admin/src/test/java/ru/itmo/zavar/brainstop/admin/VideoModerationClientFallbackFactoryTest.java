package ru.itmo.zavar.brainstop.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.admin.client.VideoModerationClient;
import ru.itmo.zavar.brainstop.admin.client.VideoModerationClientFallbackFactory;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"NFR-3.1", "NFR-3.2"})
@Layer(TestLayer.BE_UNIT)
class VideoModerationClientFallbackFactoryTest {

    private VideoModerationClientFallbackFactory factory;
    private VideoModerationClient fallbackClient;

    @BeforeEach
    void setUp() {
        factory = new VideoModerationClientFallbackFactory();
        fallbackClient = factory.create(new RuntimeException("Service unavailable"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-07: Fallback возвращает пустой список видео")
    void fallback_getUserVideos_returnsEmptyList() {
        assertThat(fallbackClient.getUserVideos(1L)).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FALLBACK-08: Fallback создается с null причиной")
    void fallback_createdWithNullCause() {
        VideoModerationClient client = factory.create(null);
        assertThat(client).isNotNull();
        assertThat(client.getUserVideos(1L)).isEmpty();
    }
}
