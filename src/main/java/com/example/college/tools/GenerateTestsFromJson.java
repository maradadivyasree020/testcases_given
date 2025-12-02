package com.example.college.tools;

import com.example.college.rag.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;

/**
 * CLI tool:
 *  1. Ingests Java source code
 *  2. Asks LLM (via RAG) to generate functional test cases in JSON
 *  3. Writes them to test-cases/attendance-functional-tests.json
 *
 * Usage (from project root):
 *   mvn -q -DskipTests exec:java -Dexec.mainClass="com.example.college.tools.GenerateTestsFromJson"
 */
public class GenerateTestsFromJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path srcMainJava = projectRoot.resolve("src/main/java");
        Path outputDir = projectRoot.resolve("test-cases");
        Files.createDirectories(outputDir);
        Path outFile = outputDir.resolve("attendance-functional-tests.json");

        System.out.println("Project root: " + projectRoot);
        System.out.println("Scanning Java sources under: " + srcMainJava);

        RagService ragService = new RagService();
        ragService.ingestJavaSources(srcMainJava);

        String question = "Generate detailed API functional test cases for all attendance-related " +
                "endpoints in this project. Focus especially on AttendanceController and " +
                "attendance/mark and attendance/mark-batch endpoints.";

        System.out.println("Calling RAG pipeline with question:\n" + question + "\n");

        String llmOutput = ragService.generateTestCasesForQuestion(question);

        // We expect LLM output to already be JSON array. If not, we just wrap it as a string.
        String finalJson = ensureJsonArray(llmOutput);

        Files.writeString(outFile, finalJson);
        System.out.println("✅ Wrote generated test cases to: " + outFile.toAbsolutePath());
    }

    private static String ensureJsonArray(String raw) throws IOException {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            // Looks like JSON array already
            JsonNode node = MAPPER.readTree(trimmed); // will throw if invalid
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        }

        // Otherwise wrap as an object with "raw" field so it is still valid JSON
        JsonNode node = MAPPER.createObjectNode().put("raw", raw);
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }
}
