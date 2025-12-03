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

        //for attendance controller
        // String question = "Generate detailed API functional test cases for all attendance-related " +
        //         "endpoints in this project. Focus especially on AttendanceController and " +
        //         "attendance/mark and attendance/mark-batch endpoints.";

        //for employee controller
        String question =
        "Generate detailed API functional test cases for all employee-related " +
        "endpoints in this project. Focus especially on EmployeeController and " +
        "/api/employe, /api/employe/{id}, /api/employee, /api/employee/{id} endpoints.";


        System.out.println("Calling RAG pipeline with question:\n" + question + "\n");

        String llmOutput = ragService.generateTestCasesForQuestion(question);

        // We expect LLM output to already be JSON array. If not, we just wrap it as a string.
        String finalJson = ensureJsonArray(llmOutput);

        Files.writeString(outFile, finalJson);
        System.out.println("✅ Wrote generated test cases to: " + outFile.toAbsolutePath());
    }

    // private static String ensureJsonArray(String raw) throws IOException {
    //     String trimmed = raw.trim();
    //     if (trimmed.startsWith("[")) {
    //         // Looks like JSON array already
    //         JsonNode node = MAPPER.readTree(trimmed); // will throw if invalid
    //         return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    //     }

    //     // Otherwise wrap as an object with "raw" field so it is still valid JSON
    //     JsonNode node = MAPPER.createObjectNode().put("raw", raw);
    //     return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    // }

    private static String ensureJsonArray(String raw) throws IOException {
    // Trim leading/trailing whitespace
    String trimmed = raw.trim();

    // Try to isolate the JSON array part: from first '[' to last ']'
    int start = trimmed.indexOf('[');
    int end = trimmed.lastIndexOf(']');

    if (start != -1 && end != -1 && start < end) {
        String arrayPart = trimmed.substring(start, end + 1);

        try {
            // Try to parse the array to verify it's valid JSON
            JsonNode node = MAPPER.readTree(arrayPart);  // may throw
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException e) {
            // If it's almost-JSON but slightly broken, keep the raw array content
            System.err.println("WARNING: LLM output is not strictly valid JSON, " +
                    "writing raw array substring. Parser error: " + e.getMessage());
            return arrayPart;
        }
    }

    // If no '[' ... ']' found at all, just return the raw text so you can inspect it
    System.err.println("WARNING: No JSON array delimiters found in LLM output. " +
            "Writing raw response to file for manual inspection.");
    return trimmed;
}

}
