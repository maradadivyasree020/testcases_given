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
 * RagService
 *
 * Responsibilities:
 *  1) Ingest Java source code into vector store
 *  2) Retrieve relevant code chunks
 *  3) Generate NEW test cases (generate mode)
 *  4) EDIT existing test cases with maximum stability (edit mode)
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
        this.codeChunker = new CodeChunker(80); // fixed chunk size for stability
        this.retriever = new RagRetriever(embeddingClient, vectorStore);
        this.promptBuilder = new PromptBuilder();
    }

    // =========================================================
    // 1️⃣ INGEST JAVA SOURCES (VECTOR STORE)
    // =========================================================
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
                String id = file + "#chunk-" + i;
                String chunk = chunks.get(i);

                float[] embedding = embeddingClient.embed(chunk);

                Map<String, String> meta = new HashMap<>();
                meta.put("file", file.toString());
                meta.put("content", chunk);

                vectorStore.upsert(id, embedding, meta);
            }

            System.out.println("Ingested: " + file + " (" + chunks.size() + " chunks)");
        } catch (Exception e) {
            System.err.println("Failed to ingest " + file + ": " + e.getMessage());
        }
    }

    // =========================================================
    // 2️⃣ GENERATE MODE (FIRST RUN / NEW CONTROLLER / NEW ENDPOINT)
    // =========================================================
    public String generateTestCasesForQuestion(String question) {
        List<Map<String, String>> snippets = retriever.retrieve(question, 6);
        String ctx = buildContext(snippets);

        String prompt = promptBuilder.buildGeneratePrompt(
                ctx,
                question,
                null
        );

        return llmClient.complete(prompt);
    }

    public String generateTestCasesForQuestion(
            String question,
            String extraTestData
    ) {
        List<Map<String, String>> snippets = retriever.retrieve(question, 6);
        String ctx = buildContext(snippets);

        String prompt = promptBuilder.buildGeneratePrompt(
                ctx,
                question,
                extraTestData
        );

        return llmClient.complete(prompt);
    }

    // =========================================================
    // 3️⃣ EDIT MODE (CONTROLLER CHANGED → UPDATE ONLY WHAT IS NEEDED)
    // =========================================================
    public String editExistingTests(
            String oldTestsJson,
            String question,
            String extraTestData
    ) {
        List<Map<String, String>> snippets = retriever.retrieve(question, 6);
        String ctx = buildContext(snippets);

        String prompt = promptBuilder.buildEditPrompt(
                oldTestsJson,
                ctx,
                extraTestData,
                question
        );

        return llmClient.complete(prompt);
    }

    // =========================================================
    // Helper: Build deterministic RAG context
    // =========================================================
    private String buildContext(List<Map<String, String>> snippets) {
        StringBuilder ctx = new StringBuilder();
        for (Map<String, String> m : snippets) {
            ctx.append("// File: ")
               .append(m.getOrDefault("file", "UnknownFile"))
               .append("\n")
               .append(m.getOrDefault("content", ""))
               .append("\n\n");
        }
        return ctx.toString();
    }
}
