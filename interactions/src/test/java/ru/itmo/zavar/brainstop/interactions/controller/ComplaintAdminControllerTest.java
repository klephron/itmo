package ru.itmo.zavar.brainstop.interactions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.dto.ComplaintDto;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintStatus;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintTargetType;
import ru.itmo.zavar.brainstop.interactions.service.ComplaintService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-7.1", "FR-7.2"})
@Layer(TestLayer.BE_UNIT)
class ComplaintAdminControllerTest {

    @Mock
    private ComplaintService complaintService;

    @InjectMocks
    private ComplaintAdminController controller;

    private CurrentUser admin;
    private CurrentUser user;

    @BeforeEach
    void setUp() {
        admin = new CurrentUser(200L, "admin", "Admin", null, Set.of(Role.ADMIN));
        user = new CurrentUser(100L, "user", "User", null, Set.of(Role.USER));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-ADM-01: Админ получает список жалоб")
    void list_asAdmin() {
        ComplaintDto complaint = new ComplaintDto(
                UUID.randomUUID(), ComplaintTargetType.VIDEO, UUID.randomUUID(), null,
                "Violation", 100L, ComplaintStatus.OPEN, null, null, Instant.now()
        );
        when(complaintService.listOpen()).thenReturn(List.of(complaint));

        ResponseEntity<List<ComplaintDto>> response = controller.list(admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(complaintService).listOpen();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-ADM-02: Обычный пользователь не может получить жалобы")
    void list_asUser_forbidden() {
        ResponseEntity<List<ComplaintDto>> response = controller.list(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(complaintService, never()).listOpen();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-ADM-03: Админ закрывает жалобу с удалением")
    void resolve_asAdmin_withDelete() {
        UUID complaintId = UUID.randomUUID();
        doNothing().when(complaintService).resolve(any(), anyLong(), anyBoolean(), anyBoolean(), any());

        ResponseEntity<Void> response = controller.resolve(complaintId, true, false, "Deleted", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(complaintService).resolve(complaintId, 200L, true, false, "Deleted");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-ADM-04: Пользователь не может закрыть жалобу")
    void resolve_asUser_forbidden() {
        UUID complaintId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.resolve(complaintId, false, false, null, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(complaintService, never()).resolve(any(), anyLong(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-ADM-05: Без авторизации запрещен доступ")
    void resolve_noAuth_forbidden() {
        UUID complaintId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.resolve(complaintId, false, false, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
