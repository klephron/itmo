package ru.itmo.zavar.brainstop.interactions.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.interactions.dto.CreateComplaintRequest;
import ru.itmo.zavar.brainstop.interactions.service.ComplaintService;

import java.util.UUID;

@RestController
@RequestMapping("/api/moderation/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Жалобы от пользователей (interactions-service)")
public class ComplaintExternalController {

    private final ComplaintService complaintService;

    @PostMapping("/videos/{videoUuid}")
    @Operation(summary = "Пожаловаться на видео")
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
    @Operation(summary = "Пожаловаться на комментарий")
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
}
