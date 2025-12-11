package ru.itmo.zavar.ticktokcoretest.search;

public interface EmbeddingService {
    float[] embed(String text);
}
