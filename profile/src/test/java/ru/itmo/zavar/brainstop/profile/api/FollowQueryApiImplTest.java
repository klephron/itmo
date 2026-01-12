package ru.itmo.zavar.brainstop.profile.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.service.FollowService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.4"})
@Layer(TestLayer.BE_UNIT)
class FollowQueryApiImplTest {

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowQueryApiImpl followQueryApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-QUERY-01: Получение подписок в пределах кандидатов")
    void getFollowingWithin_success() {
        when(followService.getFollowingWithin(100L, Set.of(200L, 201L, 202L)))
                .thenReturn(Set.of(200L, 202L));

        Set<Long> result = followQueryApi.getFollowingWithin(100L, Set.of(200L, 201L, 202L));

        assertThat(result).containsExactlyInAnyOrder(200L, 202L);
        verify(followService).getFollowingWithin(100L, Set.of(200L, 201L, 202L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FOLLOW-QUERY-02: Пустой набор кандидатов")
    void getFollowingWithin_emptyCandidates() {
        when(followService.getFollowingWithin(anyLong(), any())).thenReturn(Set.of());

        Set<Long> result = followQueryApi.getFollowingWithin(100L, Set.of());

        assertThat(result).isEmpty();
    }
}
