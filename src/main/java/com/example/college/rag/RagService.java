package com.example.college.rag;

import com.example.college.tools.EmbeddingClient;
import com.example.college.tools.LLMClient;
import com.example.college.tools.VectorStoreClient;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates:
 *  - ingesting Java code
 *  - retrieving relevant chunks
 *  - generating test cases via LLM
 */
public class RagService {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStore;
    private final LLMClient llmClient;
    private final CodeChunker codeChunker;
    private final RagRetriever retriever;
    private final PromptBuilder promptBuilder;

    public RagService() {
        this.embeddingClient = new EmbeddingClient();
        this.vectorStore = new VectorStoreClient();
        this.llmClient = new LLMClient();
        this.codeChunker = new CodeChunker(80); // 80 lines per chunk
        this.retriever = new RagRetriever(embeddingClient, vectorStore);
        this.promptBuilder = new PromptBuilder();
    }

    public void ingestJavaSources(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .forEach(this::ingestFileSafe);
        }
    }

    private void ingestFileSafe(Path file) {
        try {
            String code = Files.readString(file);
            List<String> chunks = codeChunker.chunk(code);

            for (int i = 0; i < chunks.size(); i++) {
                String id = file.toString() + "#chunk-" + i;
                String chunk = chunks.get(i);
                float[] emb = embeddingClient.embed(chunk);

                Map<String, String> meta = new HashMap<>();
                meta.put("file", file.toString());
                meta.put("content", chunk);

                vectorStore.upsert(id, emb, meta);
            }

            System.out.println("Ingested: " + file + " (" + chunks.size() + " chunks)");
        } catch (Exception e) {
            System.err.println("Failed to ingest " + file + ": " + e.getMessage());
        }
    }

public String generateTestCasesForQuestion(String question) {
    List<Map<String, String>> snippets = retriever.retrieve(question, 6);
    String prompt = promptBuilder.buildTestGenerationPrompt(question, snippets);
    return llmClient.complete(prompt);
}

public String generateTestCasesForQuestion(String question, String extraTestData) {
    List<Map<String, String>> snippets = retriever.retrieve(question, 6);

    // Build context from chunks
    StringBuilder ctx = new StringBuilder();
    for (Map<String, String> m : snippets) {
        ctx.append("// File: ").append(m.getOrDefault("file", "UnknownFile")).append("\n");
        ctx.append(m.getOrDefault("content", "")).append("\n\n");
    }

    // Build final prompt
    String prompt = promptBuilder.buildTestGenerationPrompt(
            ctx.toString(),
            question,
            extraTestData
    );

    return llmClient.complete(prompt);
}

}
