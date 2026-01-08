package ru.itmo.zavar.brainstop.transcoding.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.transcoding.service.VideoTranscodingService;

import java.util.UUID;

@RestController
@RequestMapping("/internal/transcoding")
@RequiredArgsConstructor
@Tag(name = "Internal-Transcoding", description = "Внутренний API транскодинга")
public class TranscodingInternalController {

    private final VideoTranscodingService transcoder;
    private final TranscodingJobRegistry jobRegistry;

    @PostMapping("/jobs")
    @Operation(summary = "Запустить транскодинг")
    public ResponseEntity<Void> start(@Valid @RequestBody TranscodingCommand command) throws Exception {
        if (command == null || !StringUtils.hasText(command.storageKey()) || command.jobId() == null) {
            return ResponseEntity.badRequest().build();
        }
        jobRegistry.put(command.jobId(), new TranscodingJobStatus(TranscodingJobStage.UPLOADING, 0, "Получен в транскодере", command.videoName(), command.userId()));
        transcoder.handleCommand(command);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Статус job")
    public ResponseEntity<TranscodingJobStatus> status(@PathVariable UUID jobId) {
        return jobRegistry.get(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
