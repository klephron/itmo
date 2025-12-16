package ru.itmo.zavar.brainstop.video.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;

@RestController
@RequestMapping("/internal/videos")
@RequiredArgsConstructor
@Tag(name = "Internal-Videos", description = "Внутренний API для профилей (список видео пользователя)")
public class VideoInternalController {

    private final VideoProfileApi videoProfileApi;

    @GetMapping("/users/{uploaderId}")
    @Operation(summary = "Внутренний список видео пользователя")
    public PageResponse<PublicUserVideoDto> getUserVideos(@PathVariable Long uploaderId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        Page<PublicUserVideoDto> result = this.videoProfileApi.findUserVideos(uploaderId, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }
}
