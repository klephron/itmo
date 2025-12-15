package ru.itmo.zavar.ticktokcoretest.search.embedding;

public interface EmbeddingService {
    float[] embed(String text);
}
