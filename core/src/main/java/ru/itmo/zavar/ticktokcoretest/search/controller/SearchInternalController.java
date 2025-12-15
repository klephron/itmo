package ru.itmo.zavar.ticktokcoretest.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.ticktokcoretest.common.api.PageResponse;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.ticktokcoretest.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.video.repo.FileMetadataRepository;

import java.util.List;

@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
@Tag(name = "Internal-Search", description = "Внутренний API для поиска (используется search-service)")
public class SearchInternalController {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;

    @GetMapping("/videos")
    @Operation(summary = "Внутренний поиск видео")
    public PageResponse<FileMetadataDto> searchVideos(@RequestParam("vec") String queryVecLiteral,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FileMetadataEntity> result = this.fileMetadataRepository.searchByEmbedding(queryVecLiteral, pageable);
        return new PageResponse<>(
                result.getContent().stream().map(fileMetadataMapper::toDto).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @GetMapping("/videos/suggest")
    @Operation(summary = "Внутренние подсказки поиска")
    public List<FileMetadataDto> suggest(@RequestParam("vec") String queryVecLiteral,
                                         @RequestParam(defaultValue = "5") int limit) {
        Page<FileMetadataEntity> page = this.fileMetadataRepository.searchByEmbedding(queryVecLiteral, PageRequest.of(0, limit));
        return page.getContent().stream().map(fileMetadataMapper::toDto).toList();
    }
}
