package com.example.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

public class GenerateTestsFromJson {

    // Adjust paths as per your repo layout
    private static final Path JSON_PATH =
            Paths.get("test-cases", "attendance-functional-tests.json");
    private static final Path OUTPUT_DIR =
            Paths.get("src", "test", "java", "com", "example", "college", "controller");
    private static final Path OUTPUT_FILE =
            OUTPUT_DIR.resolve("GeneratedAttendanceTests.java");

    // LLM config
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_NAME = "gpt-4.1"; // or any other model

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }

        // 1) Load JSON test cases
        List<Map<String, Object>> testCases = loadTestCases(JSON_PATH);

        // 2) Build prompt for LLM
        String prompt = buildPrompt(testCases);

        // 3) Call LLM to generate Java code
        String javaCode = callLlm(apiKey, prompt);

        // 4) Write generated code to test directory
        writeOutput(javaCode);

        System.out.println("Generated tests written to: " + OUTPUT_FILE.toAbsolutePath());
    }

    private static List<Map<String, Object>> loadTestCases(Path jsonPath) throws IOException {
        if (!Files.exists(jsonPath)) {
            throw new IllegalStateException("Test case JSON file not found: " + jsonPath.toAbsolutePath());
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(Files.readAllBytes(jsonPath),
                new TypeReference<List<Map<String, Object>>>() {});
    }

    private static String buildPrompt(List<Map<String, Object>> cases) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cases);

        return """
                Convert the following functional test cases into a single Java JUnit 5 test class.

                Requirements:
                - Package: com.example.college.controller
                - Class name: GeneratedAttendanceTests
                - Use @SpringBootTest and @AutoConfigureMockMvc
                - Use org.springframework.test.web.servlet.MockMvc
                - For each test case, create one @Test method.
                - Method name should start with the Test Case ID, e.g. TC_ATT_SINGLE_001_...
                - Use JSON body and endpoint as specified in each test case.
                - Use assertions that approximate the 'Expected Result' text.
                - Do NOT wrap code in markdown; return only raw Java code.

                Test cases JSON:
                """ + json;
    }

    private static String callLlm(String apiKey, String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String requestBodyJson = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are an expert Java developer. Generate JUnit 5 test code using MockMvc for a Spring Boot REST API."
                    },
                    {
                      "role": "user",
                      "content": %s
                    }
                  ],
                  "temperature": 0
                }
                """.formatted(
                MODEL_NAME,
                toJsonStringLiteral(prompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("LLM API error: " + response.statusCode() +
                    " body=" + response.body());
        }

        // Parse response JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> root = mapper.readValue(response.body(), new TypeReference<>() {});
        List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices returned from LLM");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        return content != null ? content.trim() : "";
    }

    // Escape for JSON string literal
    private static String toJsonStringLiteral(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private static void writeOutput(String javaCode) throws IOException {
        if (!Files.exists(OUTPUT_DIR)) {
            Files.createDirectories(OUTPUT_DIR);
        }
        Files.writeString(OUTPUT_FILE, javaCode, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
