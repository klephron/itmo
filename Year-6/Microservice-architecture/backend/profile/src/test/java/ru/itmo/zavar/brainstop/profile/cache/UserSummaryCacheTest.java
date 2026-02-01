package ru.itmo.zavar.brainstop.profile.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class UserSummaryCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;

    private UserSummaryCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cache = new UserSummaryCache(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-01: Сохранение в кеш")
    void put_savesToRedis() throws Exception {
        UserSummaryDto summary = new UserSummaryDto(100L, "User", "/avatar.jpg");

        cache.put(summary);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-02: Получение из кеша")
    void get_fromRedis() throws Exception {
        UserSummaryDto summary = new UserSummaryDto(100L, "User", "/avatar.jpg");
        String json = objectMapper.writeValueAsString(summary);
        when(valueOperations.get("profile:summary:100")).thenReturn(json);

        Optional<UserSummaryDto> result = cache.get(100L);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("User");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-03: Кеш пуст")
    void get_notInCache() {
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<UserSummaryDto> result = cache.get(100L);

        assertThat(result).isEmpty();
    }
}
