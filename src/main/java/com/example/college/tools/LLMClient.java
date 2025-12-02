package com.example.college.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple LLM client for an OpenAI-compatible API (OpenAI / OpenRouter / Groq, etc.)
 * Env vars:
 *   LLM_API_KEY   = your key
 *   LLM_BASE_URL  = https://api.openai.com/v1/chat/completions (or compatible)
 *   LLM_MODEL     = gpt-4.1-mini (or any chat model)
 */
public class LLMClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public LLMClient() {
        this.apiKey = getEnvOrThrow("LLM_API_KEY");
        this.baseUrl = System.getenv().getOrDefault(
                "LLM_BASE_URL",
                "https://api.openai.com/v1/chat/completions"
        );
        this.model = System.getenv().getOrDefault("LLM_MODEL", "gpt-4.1-mini");
    }

    private static String getEnvOrThrow(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set");
        }
        return v;
    }

    private String buildRequestBody(String prompt) throws JsonProcessingException {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        root.put("temperature", 0.1);

        return mapper.writeValueAsString(root);
    }

    public String complete(String prompt) {
        try {
            String bodyJson = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("LLM error: HTTP " + response.statusCode()
                        + " body=" + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode msg = root.path("choices").get(0).path("message").path("content");
            return msg.asText();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error calling LLM", e);
        }
    }
}
