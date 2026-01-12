package ru.itmo.zavar.brainstop.interactions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.zavar.brainstop.comment.service.CommentService;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.controller.*;
import ru.itmo.zavar.brainstop.interactions.service.ComplaintService;
import ru.itmo.zavar.brainstop.like.service.LikeService;
import ru.itmo.zavar.brainstop.like.service.ViewService;
import ru.itmo.zavar.brainstop.like.service.WatchService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@FR({"NFR-1.1", "NFR-1.6"})
@TestType(TestType.Type.BASIC)
@Layer(TestLayer.BE_INT)
class InteractionsIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private LikeService likeService;

    @Autowired(required = false)
    private CommentService commentService;

    @Autowired(required = false)
    private ViewService viewService;

    @Autowired(required = false)
    private WatchService watchService;

    @Autowired(required = false)
    private ComplaintService complaintService;

    @Autowired(required = false)
    private CommentExternalController commentController;

    @Autowired(required = false)
    private LikeExternalController likeController;

    @Autowired(required = false)
    private ViewExternalController viewController;

    @Autowired(required = false)
    private WatchExternalController watchController;

    @Autowired(required = false)
    private ComplaintExternalController complaintController;

    @Autowired(required = false)
    private InteractionsInternalController internalController;

    @Test
    @DisplayName("BE-INT-INT-01: Spring контекст загружается")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-INT-02: Все сервисы созданы")
    void servicesExist() {
        assertThat(likeService).isNotNull();
        assertThat(commentService).isNotNull();
        assertThat(viewService).isNotNull();
        assertThat(watchService).isNotNull();
        assertThat(complaintService).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-INT-03: Все контроллеры созданы")
    void controllersExist() {
        assertThat(commentController).isNotNull();
        assertThat(likeController).isNotNull();
        assertThat(viewController).isNotNull();
        assertThat(watchController).isNotNull();
        assertThat(complaintController).isNotNull();
        assertThat(internalController).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-INT-04: Security конфигурация загружена")
    void securityConfigured() {
        assertThat(context.getBean("securityFilterChain")).isNotNull();
        assertThat(context.getBean("jwtDecoder")).isNotNull();
    }

    @Test
    @DisplayName("BE-INT-INT-05: Kafka beans созданы")
    void kafkaConfigured() {
        assertThat(context.getBean(KafkaTemplate.class)).isNotNull();
    }
}
