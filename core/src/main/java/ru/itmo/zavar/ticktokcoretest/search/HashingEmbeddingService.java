package ru.itmo.zavar.ticktokcoretest.search;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
public class HashingEmbeddingService implements EmbeddingService {

    private static final int DIM = 256;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[DIM];
        }
        float[] vec = new float[DIM];
        String[] tokens = text.toLowerCase(Locale.ROOT).split("\\s+");
        for (String token : tokens) {
            int idx = positiveHash(token) % DIM;
            vec[idx] += 1.0f;
        }
        normalize(vec);
        return vec;
    }

    private void normalize(float[] v) {
        double norm = 0.0;
        for (float f : v) {
            norm += f * f;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / norm);
        }
    }

    private int positiveHash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            int h = 0;
            for (int i = 0; i < 4 && i < digest.length; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return Math.abs(h);
        } catch (NoSuchAlgorithmException e) {
            return Math.abs(token.hashCode());
        }
    }
}
