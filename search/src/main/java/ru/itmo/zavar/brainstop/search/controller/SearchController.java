package ru.itmo.zavar.brainstop.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.search.client.SearchInternalClient;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;
import com.pgvector.PGvector;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Поиск видео по тексту/эмбеддингам (отдельный сервис)")
public class SearchController {

    private final SearchInternalClient searchInternalClient;
    private final EmbeddingService embeddingService;

    @GetMapping("/videos")
    @Operation(summary = "Поиск видео", description = "Возвращает страницу совпадений")
    public ResponseEntity<PageResponse<FileMetadataDto>> searchVideos(@RequestParam("q") String query,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size) {
        String vecLiteral = new PGvector(embeddingService.embed(query)).toString();
        PageResponse<FileMetadataDto> response = this.searchInternalClient.searchVideos(vecLiteral, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/videos/suggest")
    @Operation(summary = "Подсказки поиска", description = "Топ подходящих видео")
    public ResponseEntity<List<FileMetadataDto>> suggestVideos(@RequestParam("q") String query,
                                                                @RequestParam(defaultValue = "5") int limit) {
        String vecLiteral = new PGvector(embeddingService.embed(query)).toString();
        List<FileMetadataDto> list = this.searchInternalClient.suggest(vecLiteral, limit);
        return ResponseEntity.ok(list);
    }
}
