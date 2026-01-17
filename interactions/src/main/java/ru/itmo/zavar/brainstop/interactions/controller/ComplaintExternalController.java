package ru.itmo.zavar.brainstop.interactions.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.interactions.dto.CreateComplaintRequest;
import ru.itmo.zavar.brainstop.interactions.dto.ComplaintDto;
import ru.itmo.zavar.brainstop.interactions.service.ComplaintService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/moderation/complaints")
@RequiredArgsConstructor
@Tag(name = "Жалобы", description = "Жалобы от пользователей (interactions-service)")
public class ComplaintExternalController {

    private final ComplaintService complaintService;

    private boolean isModerator(CurrentUser principal) {
        if (principal == null) return false;
        return principal.getRoles().contains(Role.ADMIN) || principal.getRoles().contains(Role.MODERATOR);
    }

    @PostMapping("/videos/{videoUuid}")
    @Operation(summary = "Создать жалобу на видео")
    public ResponseEntity<Void> complainVideo(@PathVariable UUID videoUuid,
                                              @Valid @RequestBody CreateComplaintRequest req,
                                              @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        complaintService.submitVideoComplaint(videoUuid, principal.getId(), req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/videos/{videoUuid}/comments/{commentId}")
    @Operation(summary = "Создать жалобу на комментарий")
    public ResponseEntity<Void> complainComment(@PathVariable UUID videoUuid,
                                                @PathVariable Long commentId,
                                                @Valid @RequestBody CreateComplaintRequest req,
                                                @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        complaintService.submitCommentComplaint(videoUuid, commentId, principal.getId(), req);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/admin")
    @Operation(summary = "Список открытых жалоб (для модераторов)")
    public ResponseEntity<List<ComplaintDto>> listOpen(@AuthenticationPrincipal CurrentUser principal) {
        if (!isModerator(principal)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(complaintService.listOpen());
    }

    @PostMapping("/admin/{id}/resolve")
    @Operation(summary = "Закрыть жалобу (для модераторов)")
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
