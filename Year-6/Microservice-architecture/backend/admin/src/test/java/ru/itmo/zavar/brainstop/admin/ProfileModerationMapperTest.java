package ru.itmo.zavar.brainstop.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.admin.dto.UserDetailDto;
import ru.itmo.zavar.brainstop.admin.mapper.ProfileModerationMapper;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-14.2"})
@Layer(TestLayer.BE_UNIT)
class ProfileModerationMapperTest {

    private final ProfileModerationMapper mapper = new ProfileModerationMapper();

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-01: Маппинг ProfileUserDetails в UserDetailDto")
    void toDto_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ProfileUserDetails source = new ProfileUserDetails(
                1L, "testuser", "Test User", "test@example.com",
                true, Set.of(Role.USER, Role.MODERATOR), now, "/avatar.jpg"
        );

        UserDetailDto result = mapper.toDto(source);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.displayName()).isEqualTo("Test User");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).containsExactlyInAnyOrder(Role.USER, Role.MODERATOR);
        assertThat(result.createdAt()).isEqualTo(now);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-02: Маппинг с пустыми ролями")
    void toDto_withEmptyRoles() {
        ProfileUserDetails source = new ProfileUserDetails(
                2L, "user2", "User Two", "user2@test.com",
                false, Set.of(), null, null
        );

        UserDetailDto result = mapper.toDto(source);

        assertThat(result).isNotNull();
        assertThat(result.roles()).isEmpty();
        assertThat(result.enabled()).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MAPPER-03: Маппинг с null значениями")
    void toDto_withNullValues() {
        ProfileUserDetails source = new ProfileUserDetails(
                3L, "user3", null, "user3@test.com",
                true, null, null, null
        );

        UserDetailDto result = mapper.toDto(source);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.displayName()).isNull();
        assertThat(result.roles()).isNull();
    }
}
