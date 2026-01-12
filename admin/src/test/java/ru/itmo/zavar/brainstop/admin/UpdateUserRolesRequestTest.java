package ru.itmo.zavar.brainstop.admin;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.admin.dto.UpdateUserRolesRequest;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-14.3"})
@Layer(TestLayer.BE_UNIT)
class UpdateUserRolesRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-DTO-01: Валидный запрос с ролями")
    void validRequest() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest(Set.of(Role.USER, Role.MODERATOR));

        Set<ConstraintViolation<UpdateUserRolesRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.roles()).containsExactlyInAnyOrder(Role.USER, Role.MODERATOR);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DTO-02: Невалидный запрос с пустым набором ролей")
    void invalidRequest_emptyRoles() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest(Collections.emptySet());

        Set<ConstraintViolation<UpdateUserRolesRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-DTO-03: Невалидный запрос с null ролями")
    void invalidRequest_nullRoles() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest(null);

        Set<ConstraintViolation<UpdateUserRolesRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
