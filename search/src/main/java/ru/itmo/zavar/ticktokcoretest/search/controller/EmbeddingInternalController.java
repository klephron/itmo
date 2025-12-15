package ru.itmo.zavar.ticktokcoretest.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.ticktokcoretest.common.api.search.dto.EmbeddingResponse;
import ru.itmo.zavar.ticktokcoretest.search.embedding.EmbeddingService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/internal/embedding")
@RequiredArgsConstructor
public class EmbeddingInternalController {

    private final EmbeddingService embeddingService;

    @GetMapping("/vector")
    public EmbeddingResponse embed(@RequestParam("text") String text) {
        float[] vec = embeddingService.embed(text == null ? "" : text);
        List<Float> list = new ArrayList<>(vec.length);
        for (float v : vec) {
            list.add(v);
        }
        return new EmbeddingResponse(list);
    }
}
