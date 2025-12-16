package ru.itmo.zavar.brainstop.interactions.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.interactions.dto.ComplaintDto;
import ru.itmo.zavar.brainstop.interactions.service.ComplaintService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/moderation/complaints")
@RequiredArgsConstructor
@Tag(name = "Internal-Complaints", description = "Модерация жалоб (interactions-service)")
public class ComplaintAdminController {

    private final ComplaintService complaintService;

    private boolean isModerator(CurrentUser principal) {
        if (principal == null) return false;
        return principal.getRoles().contains(Role.ADMIN) || principal.getRoles().contains(Role.MODERATOR);
    }

    @GetMapping
    @Operation(summary = "Список открытых жалоб")
    public ResponseEntity<List<ComplaintDto>> list(@AuthenticationPrincipal CurrentUser principal) {
        if (!isModerator(principal)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(complaintService.listOpen());
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Закрыть жалобу (можно удалить объект)")
    public ResponseEntity<Void> resolve(@PathVariable UUID id,
                                        @RequestParam(name = "deleteTarget", defaultValue = "false") boolean deleteTarget,
                                        @RequestParam(name = "reject", defaultValue = "false") boolean reject,
                                        @RequestParam(name = "resolution", required = false) String resolution,
                                        @AuthenticationPrincipal CurrentUser principal) {
        if (!isModerator(principal)) {
            return ResponseEntity.status(403).build();
        }
        complaintService.resolve(id, principal.getId(), deleteTarget, reject, resolution);
        return ResponseEntity.noContent().build();
    }
}
