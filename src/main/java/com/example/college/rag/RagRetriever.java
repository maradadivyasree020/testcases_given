package com.example.college.rag;

import com.example.college.tools.EmbeddingClient;
import com.example.college.tools.VectorStoreClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RagRetriever {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStore;

    public RagRetriever(EmbeddingClient embeddingClient, VectorStoreClient vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public List<Map<String, String>> retrieve(String question, int topK) {
        float[] qEmbedding = embeddingClient.embed(question);
        List<VectorStoreClient.Entry> hits = vectorStore.query(qEmbedding, topK);

        return hits.stream()
                .map(e -> e.metadata)
                .collect(Collectors.toList());
    }
}
