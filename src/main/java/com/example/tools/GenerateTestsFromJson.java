package com.example.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

public class GenerateTestsFromJson {

    // JSON with your functional test cases
    private static final Path JSON_PATH =
            Paths.get("test-cases", "attendance-functional-tests.json");

    // Where to write generated JUnit tests
    private static final Path OUTPUT_DIR =
            Paths.get("src/test/java/com/example/college/controller");
    private static final Path OUTPUT_FILE =
            OUTPUT_DIR.resolve("GeneratedAttendanceTests.java");

    // OpenAI chat completions endpoint
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    // Use a valid model
    private static final String MODEL = "gpt-4o-mini"; // or "gpt-3.5-turbo"

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            System.err.println("[GenerateTestsFromJson] FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            // do NOT System.exit(1) so CI doesn't fail just because generation failed
        }
    }

    private static void run() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[GenerateTestsFromJson] OPENAI_API_KEY not set. Skipping generation.");
            return;
        }

        if (!Files.exists(JSON_PATH)) {
            System.out.println("[GenerateTestsFromJson] JSON file not found at "
                    + JSON_PATH.toAbsolutePath() + ". Skipping generation.");
            return;
        }

        System.out.println("[GenerateTestsFromJson] Using JSON: " + JSON_PATH.toAbsolutePath());
        System.out.println("[GenerateTestsFromJson] Output file: " + OUTPUT_FILE.toAbsolutePath());

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> cases = mapper.readValue(
                Files.readAllBytes(JSON_PATH),
                new TypeReference<>() {}
        );

        String jsonCases = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cases);

        String prompt =
                "Convert the following functional test cases into a single Java JUnit 5 test class.\n" +
                "\n" +
                "Requirements:\n" +
                "- Package: com.example.college.controller\n" +
                "- Class name: GeneratedAttendanceTests\n" +
                "- Use @SpringBootTest and @AutoConfigureMockMvc\n" +
                "- Use org.springframework.test.web.servlet.MockMvc\n" +
                "- Use file test-cases/attendance-functional-tests.json to generate JUnit TestCaes. \n"+
                "- For each test case, create one @Test method.\n" +
                "- Method name should start with the Test Case ID (e.g. TC_ATT_SINGLE_001_...)\n" +
                "- Use endpoint + body from Input.\n" +
                "- Assert according to Expected Result.\n" +
                "- Return ONLY raw Java code (no markdown).\n" +
                "\n" +
                "Test cases JSON:\n" + jsonCases;

        String javaCode = callLLM(apiKey, prompt);

        // Just in case the model returns markdown, strip ``` fences
        javaCode = stripCodeFences(javaCode).trim();

        Files.createDirectories(OUTPUT_DIR);
        Files.writeString(OUTPUT_FILE, javaCode, StandardCharsets.UTF_8);

        System.out.println("[GenerateTestsFromJson] Successfully generated tests.");
    }

    private static String callLLM(String apiKey, String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String payload = """
        {
          "model": "%s",
          "messages": [
            {
              "role": "system",
              "content": "You are an expert Java developer. Generate JUnit 5 tests for a Spring Boot REST API using MockMvc."
            },
            {
              "role": "user",
              "content": %s
            }
          ],
          "temperature": 0
        }
        """.formatted(MODEL, toJsonString(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        System.out.println("[GenerateTestsFromJson] Calling OpenAI model: " + MODEL);

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[GenerateTestsFromJson] OpenAI HTTP status: " + response.statusCode());

        if (response.statusCode() / 100 != 2) {
            System.err.println("[GenerateTestsFromJson] OpenAI error body: " + response.body());
            throw new IllegalStateException("OpenAI API call failed with status " + response.statusCode());
        }

        // Parse `choices[0].message.content`
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> root = mapper.readValue(response.body(), new TypeReference<>() {});
        var choices = (List<Map<String, Object>>) root.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("No choices returned by OpenAI");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        return content != null ? content : "";
    }

    private static String toJsonString(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private static String stripCodeFences(String s) {
        if (s.startsWith("```")) {
            int firstNewLine = s.indexOf('\n');
            if (firstNewLine != -1) {
                s = s.substring(firstNewLine + 1);
            }
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.lastIndexOf("```"));
        }
        return s;
    }
}
