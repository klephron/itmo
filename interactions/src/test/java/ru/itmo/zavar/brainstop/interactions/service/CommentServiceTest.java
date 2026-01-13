package ru.itmo.zavar.brainstop.interactions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.comment.model.CommentEntity;
import ru.itmo.zavar.brainstop.comment.repo.CommentRepository;
import ru.itmo.zavar.brainstop.comment.service.CommentService;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-6.1", "FR-6.2"})
@Layer(TestLayer.BE_UNIT)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private VideoMetadataPublicApi videoMetadataPublicApi;

    @Mock
    private UserProfilePublicApi userProfilePublicApi;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CommentService commentService;

    private UUID videoUuid;
    private VideoMetadataSummary videoMeta;
    private CommentEntity testComment;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        videoMeta = new VideoMetadataSummary(
                1L,              // id
                videoUuid,       // uuid
                5L,              // likesCount
                100L,            // viewsCount
                10L,             // uploaderId
                "Test Uploader", // uploaderName
                "video/mp4",     // contentType
                1000000L,        // contentSize
                "thumb.jpg"      // thumbnailKey
        );

        testComment = new CommentEntity();
        testComment.setId(1L);
        testComment.setVideoUuid(videoUuid);
        testComment.setAuthorId(100L);
        testComment.setAuthorName("Test Author");
        testComment.setText("Great video!");
        testComment.setCreatedAt(Instant.now());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-01: Создание комментария")
    void addComment_success() {
        CreateCommentRequest request = new CreateCommentRequest("New comment");

        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(commentRepository.save(any(CommentEntity.class))).thenReturn(testComment);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        CommentDto result = commentService.addComment(videoUuid, request, 100L, "Author", "/avatar.jpg");

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Great video!");
        assertThat(result.authorId()).isEqualTo(100L);
        assertThat(result.mine()).isTrue();
        verify(commentRepository).save(any(CommentEntity.class));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-02: Получение комментариев с пагинацией")
    void listComments_withPagination() {
        Page<CommentEntity> page = new PageImpl<>(List.of(testComment));
        UserSummaryDto author = new UserSummaryDto(100L, "Author", "/avatar.jpg");

        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(commentRepository.findByVideoUuid(any(UUID.class), any())).thenReturn(page);
        when(userProfilePublicApi.findUserSummary(anyLong())).thenReturn(Optional.of(author));

        Page<CommentDto> result = commentService.listComments(videoUuid, 100L, PageRequest.of(0, 20), org.springframework.data.domain.Sort.Direction.DESC);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).text()).isEqualTo("Great video!");
        assertThat(result.getContent().get(0).mine()).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-03: Удаление своего комментария")
    void deleteComment_ownComment() {
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        commentService.deleteComment(videoUuid, 1L, 100L, false);

        verify(commentRepository).delete(testComment);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMM-04: Удаление чужого комментария запрещено")
    void deleteComment_notOwner_throws() {
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.deleteComment(videoUuid, 1L, 999L, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forbidden");

        verify(commentRepository, never()).delete(any(CommentEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-05: Модератор может удалить любой комментарий")
    void deleteComment_moderator_success() {
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        commentService.deleteComment(videoUuid, 1L, 999L, true);

        verify(commentRepository).delete(testComment);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMM-06: Удаление комментария несуществующего видео")
    void deleteComment_wrongVideo_throws() {
        UUID otherVideoUuid = UUID.randomUUID();
        when(commentRepository.findById(anyLong())).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.deleteComment(otherVideoUuid, 1L, 100L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to this video");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMM-07: Удаление несуществующего комментария")
    void deleteComment_notFound_throws() {
        when(commentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(videoUuid, 999L, 100L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment not found");
    }
}
