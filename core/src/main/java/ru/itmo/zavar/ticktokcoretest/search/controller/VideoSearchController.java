package ru.itmo.zavar.ticktokcoretest.search.controller;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.ticktokcoretest.search.EmbeddingService;
import ru.itmo.zavar.ticktokcoretest.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.ticktokcoretest.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.ticktokcoretest.video.model.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.video.repo.FileMetadataRepository;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class VideoSearchController {

    private final EmbeddingService embeddingService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;

    @GetMapping("/videos")
    public ResponseEntity<Page<FileMetadataDto>> searchVideos(@RequestParam("q") String query, Pageable pageable) {
        float[] vec = this.embeddingService.embed(query);
        String literal = new PGvector(vec).toString();
        Page<FileMetadataEntity> page = this.fileMetadataRepository.searchByEmbedding(literal, pageable);
        return ResponseEntity.ok(page.map(fileMetadataMapper::toDto));
    }

    @GetMapping("/videos/suggest")
    public ResponseEntity<List<FileMetadataDto>> suggestVideos(@RequestParam("q") String query) {
        float[] vec = this.embeddingService.embed(query);
        String literal = new PGvector(vec).toString();
        Page<FileMetadataEntity> page = this.fileMetadataRepository.searchByEmbedding(literal, Pageable.ofSize(5));
        return ResponseEntity.ok(page.getContent().stream().map(fileMetadataMapper::toDto).toList());
    }
}
