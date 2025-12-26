package com.example.college.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Build;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple embedding client for OpenAI-compatible embeddings.
 * Set env vars:
 *   EMBEDDINGS_API_KEY  (if different from LLM_API_KEY; otherwise it will reuse)
 *   EMBEDDINGS_BASE_URL (default: https://api.openai.com/v1/embeddings)
 *   EMBEDDINGS_MODEL    (e.g., text-embedding-3-large)
 */
public class EmbeddingClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public EmbeddingClient() {
        String api = System.getenv("EMBEDDINGS_API_KEY");
        if (api == null || api.isBlank()) {
            api = System.getenv("LLM_API_KEY");
        }
        if (api == null || api.isBlank()) {
            throw new IllegalStateException("EMBEDDINGS_API_KEY or LLM_API_KEY env var is required");
        }
        this.apiKey = api;
        this.baseUrl = System.getenv().getOrDefault("EMBEDDINGS_BASE_URL",
                "https://api.openai.com/v1/embeddings");
        this.model = System.getenv().getOrDefault("EMBEDDINGS_MODEL", "text-embedding-3-large");
    }

    public float[] embed(String text) {
        try {
            // Build JSON request body
            String bodyJson = mapper.createObjectNode()
                    .put("model", model)
                    .put("input", text)
                    .toString();
            // Build POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Embedding error: HTTP " + response.statusCode()
                        + " body=" + response.body());
            }

            // Parse embedding array- numerical vector (float[]) returned by the API representing the meaning of your text.
            JsonNode root = mapper.readTree(response.body());
            JsonNode arr = root.path("data").get(0).path("embedding");

            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = (float) arr.get(i).asDouble();
            }
            return vec;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error calling embeddings API", e);
        }
    }
}
