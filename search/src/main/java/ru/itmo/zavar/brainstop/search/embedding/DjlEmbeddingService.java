package ru.itmo.zavar.brainstop.search.embedding;

import ai.djl.ModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Primary
@Slf4j
public class DjlEmbeddingService implements EmbeddingService {

    private ZooModel<String, float[]> model;
    private final ReentrantLock lock = new ReentrantLock();

    @PostConstruct
    public void init() {
        lock.lock();
        try {
            if (model != null) return;
            log.info("Loading local embedding model (sentence-transformers/all-MiniLM-L6-v2) via DJL...");
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optEngine("PyTorch")
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .build();
            model = ModelZoo.loadModel(criteria);
            log.info("Embedding model loaded: {}", model.getName());
        } catch (IOException | ModelException e) {
            log.error("Failed to load embedding model, fallback will be used", e);
            model = null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[384];
        }
        ZooModel<String, float[]> m = model;
        if (m == null) {
            return new HashingEmbeddingService().embed(text);
        }
        lock.lock();
        try (var predictor = m.newPredictor()) {
            float[] vec = predictor.predict(text);
            return normalize(vec);
        } catch (TranslateException e) {
            log.error("Embedding failed, fallback to hashing", e);
            return new HashingEmbeddingService().embed(text);
        } finally {
            lock.unlock();
        }
    }

    private float[] normalize(float[] vec) {
        if (vec == null || vec.length == 0) {
            return vec;
        }
        double norm = 0.0;
        for (float v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return vec;
        }
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            out[i] = (float) (vec[i] / norm);
        }
        return out;
    }
}
