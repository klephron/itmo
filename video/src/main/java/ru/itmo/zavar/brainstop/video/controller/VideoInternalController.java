package ru.itmo.zavar.brainstop.video.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.HashSet;
import java.util.Set;

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
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(value = "sort", required = false) String sortParam,
                                                          @RequestParam(value = "userId", required = false) Long currentUserId) {
        Pageable effective = sanitizePageable(page, size, sortParam);
        Page<PublicUserVideoDto> result = this.videoProfileApi.findUserVideos(uploaderId, currentUserId, effective);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    private Pageable sanitizePageable(int page, int size, String sortParam) {
        Pageable p = PageRequest.of(page, size);

        Set<String> allowed = new HashSet<>(Set.of(
                "uploadedAt",
                "likesCount",
                "viewsCount",
                "id"
        ));

        Sort sort = Sort.by(Sort.Order.desc("uploadedAt"));

        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",");
            String prop = parts.length > 0 ? parts[0] : "uploadedAt";
            String dir = parts.length > 1 ? parts[1] : "desc";
            if (allowed.contains(prop)) {
                sort = "asc".equalsIgnoreCase(dir)
                        ? Sort.by(Sort.Order.asc(prop))
                        : Sort.by(Sort.Order.desc(prop));
            }
        }

        return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
    }
}
