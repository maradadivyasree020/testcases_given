// package com.example.college.tools;

// import com.example.college.rag.RagService;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.node.ArrayNode;

// import java.io.IOException;
// import java.nio.file.*;
// import java.util.List;

// public class GenerateTestsFromJson {

//     private static final ObjectMapper MAPPER = new ObjectMapper();

//     public static void main(String[] args) {
//         try {
//             Path projectRoot = Paths.get("").toAbsolutePath();
//             Path srcMainJava = projectRoot.resolve("src/main/java");
//             Path outputDir = projectRoot.resolve("test-cases");
//             Files.createDirectories(outputDir);

//             Path outFile = outputDir.resolve("all-tests.json");

//             System.out.println("Project root: " + projectRoot);
//             System.out.println("Scanning Java sources under: " + srcMainJava);

//             RagService ragService = new RagService();
//             ragService.ingestJavaSources(srcMainJava);

//             // ➤ Add ANY number of questions here
//             List<String> questions = List.of(
//                 "Generate detailed API functional test cases for AttendanceController and all attendance endpoints.",
//                 "Generate detailed API functional test cases for EmployeeController and all employee endpoints."
//                 // Add more controllers/questions here
//             );

//             // Load existing file (or create empty array)
//             ArrayNode allTests = readArrayFromFile(outFile);

//             for (String question : questions) {
//                 System.out.println("\n====================================");
//                 System.out.println("Calling RAG pipeline for question:");
//                 System.out.println(question);
//                 System.out.println("====================================\n");

//                 String llmOutput = ragService.generateTestCasesForQuestion(question);
//                 ArrayNode newTests = parseArrayFromLLM(llmOutput);

//                 System.out.println("Appending " + newTests.size() + " test cases...");
//                 allTests.addAll(newTests);
//             }

//             // Write final merged array
//             String pretty = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(allTests);
//             Files.writeString(outFile, pretty);

//             System.out.println("\n✅ Successfully wrote test cases to: " + outFile.toAbsolutePath());
//             System.out.println("Total test cases in file: " + allTests.size());

//         } catch (Exception e) {
//             System.err.println("❌ Error while generating test cases: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }

//     private static ArrayNode readArrayFromFile(Path file) {
//         ArrayNode result = MAPPER.createArrayNode();
//         if (!Files.exists(file)) return result;
//         try {
//             JsonNode node = MAPPER.readTree(Files.readString(file));
//             if (node.isArray()) result.addAll((ArrayNode) node);
//         } catch (Exception e) {
//             System.err.println("WARNING: Could not parse existing file. Using empty array.");
//         }
//         return result;
//     }

//     private static ArrayNode parseArrayFromLLM(String raw) {
//         ArrayNode empty = MAPPER.createArrayNode();
//         if (raw == null) return empty;

//         int s = raw.indexOf('[');
//         int e = raw.lastIndexOf(']');
//         if (s == -1 || e == -1 || s >= e) {
//             System.err.println("WARNING: LLM did not return JSON array.");
//             return empty;
//         }

//         String arr = raw.substring(s, e + 1);

//         try {
//             JsonNode node = MAPPER.readTree(arr);
//             if (node.isArray()) return (ArrayNode) node;
//         } catch (Exception ex) {
//             System.err.println("WARNING: LLM JSON invalid, skipping. Error: " + ex.getMessage());
//         }
//         return empty;
//     }
// }

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

            // Load existing tests (single big array)
            ArrayNode allTests = readArrayFromFile(testsFile);

            // Load controller → hash map
            Map<String, String> meta = readMetadata(metaFile);

            // 🔹 Define controllers you care about: id, question, code file(s)
            ControllerSpec[] specs = new ControllerSpec[] {
                    new ControllerSpec(
                            "attendance",
                            "Generate detailed API functional test cases for all attendance-related endpoints " +
                            "in this project. Focus especially on AttendanceController and " +
                            "attendance/mark and attendance/mark-batch endpoints.",
                            List.of("src/main/java/com/example/college/controller/AttendanceController.java")
                    ),
                    new ControllerSpec(
                            "employee",
                            "Generate detailed API functional test cases for all employee-related endpoints " +
                            "in this project. Focus especially on EmployeeController and " +
                            "/api/employe, /api/employe/{id}, /api/employee, /api/employee/{id} endpoints.",
                            List.of("src/main/java/com/example/college/controller/EmployeeController.java")
                    )
                    // ➕ Add more controllers here later if needed
            };

            for (ControllerSpec spec : specs) {
                String controllerId = spec.id();
                String question     = spec.question();

                String currentHash  = computeHashForFiles(projectRoot, spec.codeFiles());

                String previousHash = meta.get(controllerId);

                if (currentHash != null && currentHash.equals(previousHash)) {
                    System.out.println("ℹ No changes detected in controller '" + controllerId +
                                       "'. Skipping test generation.");
                    continue;
                }

                System.out.println("\n====================================");
                System.out.println("Changes detected for controller: " + controllerId);
                System.out.println("QUESTION: " + question);
                System.out.println("====================================\n");

                String llmOutput = ragService.generateTestCasesForQuestion(question);
                ArrayNode newTests = parseArrayFromLLM(llmOutput);

                System.out.println("Appending " + newTests.size() + " new test cases for controller '" +
                                   controllerId + "'...");

                allTests.addAll(newTests);

                // Update metadata
                if (currentHash != null) {
                    meta.put(controllerId, currentHash);
                }
            }

            // Write back test cases
            String prettyTests = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(allTests);
            Files.writeString(testsFile, prettyTests);
            System.out.println("\n✅ Wrote all test cases to: " + testsFile.toAbsolutePath());
            System.out.println("Total test cases in file: " + allTests.size());

            // Write back metadata
            writeMetadata(metaFile, meta);
            System.out.println("✅ Updated metadata: " + metaFile.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Error while generating tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------- Helpers -------

    /** Controller specification record. */
    private record ControllerSpec(String id, String question, List<String> codeFiles) {}

    /** Read existing array file or return empty array. */
    private static ArrayNode readArrayFromFile(Path file) {
        ArrayNode result = MAPPER.createArrayNode();
        if (!Files.exists(file)) return result;
        try {
            String content = Files.readString(file);
            JsonNode node = MAPPER.readTree(content);
            if (node != null && node.isArray()) {
                result.addAll((ArrayNode) node);
            } else {
                System.err.println("WARNING: Existing tests file is not a JSON array. Ignoring content.");
            }
        } catch (IOException e) {
            System.err.println("WARNING: Failed to read/parse existing tests file. Starting fresh. " +
                               "Error: " + e.getMessage());
        }
        return result;
    }

    /** Read controller→hash metadata or return empty map. */
    private static Map<String, String> readMetadata(Path metaFile) {
        Map<String, String> map = new HashMap<>();
        if (!Files.exists(metaFile)) return map;
        try {
            String content = Files.readString(metaFile);
            JsonNode node = MAPPER.readTree(content);
            if (node != null && node.isObject()) {
                node.fields().forEachRemaining(entry -> {
                    map.put(entry.getKey(), entry.getValue().asText());
                });
            }
        } catch (IOException e) {
            System.err.println("WARNING: Failed to read metadata file. Starting with empty metadata. " +
                               "Error: " + e.getMessage());
        }
        return map;
    }

    /** Write controller→hash metadata. */
    private static void writeMetadata(Path metaFile, Map<String, String> meta) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        for (Map.Entry<String, String> e : meta.entrySet()) {
            root.put(e.getKey(), e.getValue());
        }
        String pretty = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        Files.writeString(metaFile, pretty);
    }

    /** Compute SHA-256 hash of concatenated contents of given code files (relative paths). */
    private static String computeHashForFiles(Path projectRoot, List<String> relativePaths) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String rel : relativePaths) {
                Path p = projectRoot.resolve(rel);
                if (!Files.exists(p)) {
                    System.err.println("WARNING: Code file not found for hashing: " + p);
                    continue;
                }
                byte[] bytes = Files.readAllBytes(p);
                digest.update(bytes);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("WARNING: Failed to compute hash for controller files. " +
                               "Error: " + e.getMessage());
            return null;
        }
    }

    /** Extract first [ ... ] from LLM output and parse as JSON array; returns empty array on failure. */
    private static ArrayNode parseArrayFromLLM(String raw) {
        ArrayNode empty = MAPPER.createArrayNode();
        if (raw == null) return empty;

        String trimmed = raw.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');

        if (start == -1 || end == -1 || start >= end) {
            System.err.println("WARNING: LLM output does not contain JSON array delimiters. Raw:\n" + raw);
            return empty;
        }

        String arrayPart = trimmed.substring(start, end + 1);

        try {
            JsonNode node = MAPPER.readTree(arrayPart);
            if (node.isArray()) {
                return (ArrayNode) node;
            } else {
                System.err.println("WARNING: LLM array part is not a JSON array. Snippet:\n" + arrayPart);
                return empty;
            }
        } catch (IOException e) {
            System.err.println("WARNING: Failed to parse LLM JSON array. Error: " + e.getMessage());
            System.err.println("Raw snippet:\n" + arrayPart);
            return empty;
        }
    }
}
