package ru.itmo.zavar.brainstop.profile.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSummaryCache {

    private static final Duration SUMMARY_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "profile:summary:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<UserSummaryDto> get(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String payload = redisTemplate.opsForValue().get(key(userId));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, UserSummaryDto.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void put(UserSummaryDto summary) {
        if (summary == null || summary.id() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(summary);
            redisTemplate.opsForValue().set(key(summary.id()), payload, SUMMARY_TTL);
        } catch (JsonProcessingException e) {
            return;
        }
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
