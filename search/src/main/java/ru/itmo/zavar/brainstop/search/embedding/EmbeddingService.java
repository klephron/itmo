package ru.itmo.zavar.brainstop.search.embedding;

public interface EmbeddingService {
    float[] embed(String text);
}
