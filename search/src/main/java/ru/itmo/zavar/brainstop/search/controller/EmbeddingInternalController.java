package ru.itmo.zavar.brainstop.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/internal/embedding")
@RequiredArgsConstructor
@Tag(name = "Internal-Embedding", description = "Векторизация текста (для внутренних вызовов)")
public class EmbeddingInternalController {

    private final EmbeddingService embeddingService;

    @GetMapping("/vector")
    @Operation(summary = "Получить вектор для текста")
    public EmbeddingResponse embed(@RequestParam("text") String text) {
        float[] vec = embeddingService.embed(text == null ? "" : text);
        List<Float> list = new ArrayList<>(vec.length);
        for (float v : vec) {
            list.add(v);
        }
        return new EmbeddingResponse(list);
    }
}
