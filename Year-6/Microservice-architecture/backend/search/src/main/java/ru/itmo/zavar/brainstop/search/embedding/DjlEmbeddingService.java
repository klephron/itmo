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

    private static final int EMBEDDING_DIM = 1024;

    private ZooModel<String, float[]> model;
    private final ReentrantLock lock = new ReentrantLock();

    @PostConstruct
    public void init() {
        lock.lock();
        try {
            if (model != null) return;
            String[] modelUrls = {
                    "djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
                    "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
            };
            for (String url : modelUrls) {
                model = tryLoad(url);
                if (model != null) {
                    log.info("Embedding model loaded: {}", model.getName());
                    break;
                }
            }
        } catch (RuntimeException e) {
            log.error("Failed to load embedding model, fallback will be used", e);
            model = null;
        } finally {
            lock.unlock();
        }
    }

    private ZooModel<String, float[]> tryLoad(String modelUrl) {
        try {
            log.info("Loading local embedding model (paraphrase-multilingual-MiniLM-L12-v2) via DJL from {}...", modelUrl);
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optEngine("PyTorch")
                    .optModelUrls(modelUrl)
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .build();
            return ModelZoo.loadModel(criteria);
        } catch (IOException | ModelException | RuntimeException e) {
            log.warn("Failed to load embedding model from {}: {}", modelUrl, e.toString());
            return null;
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[EMBEDDING_DIM];
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
        float[] sized = ensureSize(vec, EMBEDDING_DIM);
        double norm = 0.0;
        for (float v : sized) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return sized;
        }
        float[] out = new float[sized.length];
        for (int i = 0; i < sized.length; i++) {
            out[i] = (float) (sized[i] / norm);
        }
        return out;
    }

    private float[] ensureSize(float[] vec, int dim) {
        if (dim <= 0) {
            return new float[0];
        }
        if (vec == null) {
            return new float[dim];
        }
        if (vec.length == dim) {
            return vec;
        }
        float[] out = new float[dim];
        System.arraycopy(vec, 0, out, 0, Math.min(vec.length, dim));
        return out;
    }
}
