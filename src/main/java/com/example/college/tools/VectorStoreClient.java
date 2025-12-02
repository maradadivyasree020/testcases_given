package com.example.college.tools;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VectorStoreClient {

    public static class Entry {
        public final String id;
        public final float[] embedding;
        public final Map<String, String> metadata;

        public Entry(String id, float[] embedding, Map<String, String> metadata) {
            this.id = id;
            this.embedding = embedding;
            this.metadata = metadata;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void upsert(String id, float[] embedding, Map<String, String> metadata) {
        store.put(id, new Entry(id, embedding, metadata));
    }

    //query method - similar to search
    public List<Entry> query(float[] queryEmbedding, int topK) {
        if (store.isEmpty()) return Collections.emptyList();

        List<Map.Entry<String, Entry>> entries = new ArrayList<>(store.entrySet());
        // Custom comparator:
// Computes similarity between queryEmbedding and each stored embedding.
// simB and simA are the similarities for b and a.
// Double.compare(simB, simA) sorts so that higher similarity comes first (descending order).
        entries.sort((a, b) -> {
            double simB = cosineSimilarity(queryEmbedding, b.getValue().embedding);
            double simA = cosineSimilarity(queryEmbedding, a.getValue().embedding);
            return Double.compare(simB, simA);
        });

        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, entries.size()); i++) {
            result.add(entries.get(i).getValue());
        }
        return result;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
