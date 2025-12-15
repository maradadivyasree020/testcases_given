package com.example.college.tools;

import com.example.college.rag.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public class GenerateTestsFromJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path srcMainJava = projectRoot.resolve("src/main/java");
            Path outputDir = projectRoot.resolve("test-cases");
            Files.createDirectories(outputDir);

            Path testsFile = outputDir.resolve("all-tests.json");
            Path metaFile  = outputDir.resolve("test-metadata.json");

            System.out.println("Project root: " + projectRoot);
            System.out.println("Scanning Java sources under: " + srcMainJava);

            RagService ragService = new RagService();
            ragService.ingestJavaSources(srcMainJava);

            ArrayNode allTests = readArrayFromFile(testsFile);
            Map<String, String> meta = readMetadata(metaFile);

            ControllerSpec[] specs = new ControllerSpec[]{
                new ControllerSpec(
                    "attendance",
                    "Generate detailed API functional test cases for all attendance-related endpoints " +
                    "in this project. Focus especially on AttendanceController and endpoints " +
                    "/attendance/mark and /attendance/mark-batch.",
                    List.of("src/main/java/com/example/college/controller/AttendanceController.java"),
                    "src/main/resources/testdata/attendance-testdata.xlsx"
                ),
                new ControllerSpec(
                    "employee",
                    "Generate detailed API functional test cases for all employee-related endpoints " +
                    "in this project. Focus especially on EmployeeController and endpoints " +
                    "/api/employe, /api/employe/{id}, /api/employee, /api/employee/{id}.",
                    List.of("src/main/java/com/example/college/controller/EmployeeController.java"),
                    "src/main/resources/testdata/employee-testdata.xlsx"
                )
            };

            for (ControllerSpec spec : specs) {
                String controllerId = spec.id();
                String question     = spec.question();

                String currentHash  = computeHashForFiles(projectRoot, spec.codeFiles());
                String previousHash = meta.get(controllerId);

                if (currentHash != null && currentHash.equals(previousHash)) {
                    System.out.println("ℹ No changes detected for controller '" + controllerId + "'");
                    continue;
                }

                System.out.println("\n====================================");
                System.out.println("Generating tests for: " + controllerId);
                System.out.println("====================================");

                Path excelPath = projectRoot.resolve(spec.testDataPath());
                String testDataJson =
                        ExcelTestDataLoader.loadAsJsonArrayString(excelPath, "Sheet1");

                String llmOutput =
                        ragService.generateTestCasesForQuestion(question, testDataJson);

                ArrayNode newTests = parseArrayFromLLM(llmOutput);

                // -----------------------------
                // REPLACEMENT LOGIC (NO DUPLICATES)
                // -----------------------------

                ArrayNode retained = MAPPER.createArrayNode();

                for (JsonNode existing : allTests) {
                    if (existing.has("__generated_by")
                        && controllerId.equals(existing.get("__generated_by").asText())) {
                        continue; // remove old generated tests
                    }
                    retained.add(existing);
                }

                for (JsonNode t : newTests) {
                    if (t.isObject()) {
                        ((ObjectNode) t).put("__generated_by", controllerId);
                    }
                }

                allTests.removeAll();
                allTests.addAll(retained);
                allTests.addAll(newTests);

                meta.put(controllerId, currentHash);
                System.out.println("✔ Replaced generated tests for controller: " + controllerId);
            }

            Files.writeString(
                testsFile,
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(allTests)
            );

            writeMetadata(metaFile, meta);

            System.out.println("\n✅ Test generation complete");
            System.out.println("Total test cases: " + allTests.size());

        } catch (Exception e) {
            System.err.println("❌ Error generating tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------

    private record ControllerSpec(
        String id,
        String question,
        List<String> codeFiles,
        String testDataPath
    ) {}

    private static ArrayNode readArrayFromFile(Path file) {
        ArrayNode result = MAPPER.createArrayNode();
        if (!Files.exists(file)) return result;

        try {
            JsonNode node = MAPPER.readTree(Files.readString(file));
            if (node.isArray()) result.addAll((ArrayNode) node);
        } catch (Exception e) {
            System.err.println("WARNING: Failed to read test file.");
        }
        return result;
    }

    private static Map<String, String> readMetadata(Path metaFile) {
        Map<String, String> map = new HashMap<>();
        if (!Files.exists(metaFile)) return map;

        try {
            JsonNode node = MAPPER.readTree(Files.readString(metaFile));
            node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
        } catch (Exception e) {
            System.err.println("WARNING: Failed to read metadata.");
        }
        return map;
    }

    private static void writeMetadata(Path metaFile, Map<String, String> meta) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        meta.forEach(root::put);
        Files.writeString(metaFile,
            MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    }

    private static String computeHashForFiles(Path root, List<String> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String rel : files) {
                Path p = root.resolve(rel);
                if (Files.exists(p)) digest.update(Files.readAllBytes(p));
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static ArrayNode parseArrayFromLLM(String raw) {
        ArrayNode empty = MAPPER.createArrayNode();
        if (raw == null) return empty;

        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');

        if (start < 0 || end <= start) return empty;

        try {
            JsonNode node = MAPPER.readTree(raw.substring(start, end + 1));
            if (node.isArray()) return (ArrayNode) node;
        } catch (Exception ignored) {}

        return empty;
    }
}
