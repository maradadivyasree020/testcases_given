// package com.example.college.tools;

// import com.example.college.rag.RagService;

// import java.nio.file.*;
// import java.util.List;

// /**
//  * CLI tool:
//  *  1. Ingests Java source code
//  *  2. Asks LLM (via RAG) to generate functional test cases in JSON
//  *  3. Writes EACH question's test cases into a separate file under test-cases/
//  *
//  * Usage (from project root):
//  *   mvn -q -DskipTests exec:java
//  */
// public class GenerateTestsFromJson {

//     public static void main(String[] args) {
//         try {
//             Path projectRoot = Paths.get("").toAbsolutePath();
//             Path srcMainJava = projectRoot.resolve("src/main/java");
//             Path outputDir = projectRoot.resolve("test-cases");
//             Files.createDirectories(outputDir);

//             System.out.println("Project root: " + projectRoot);
//             System.out.println("Scanning Java sources under: " + srcMainJava);

//             RagService ragService = new RagService();
//             ragService.ingestJavaSources(srcMainJava);

//             // ✅ Add any number of questions here
//             List<String> questions = List.of(
//                     // Attendance
//                     "Generate detailed API functional test cases for all attendance-related endpoints " +
//                             "in this project. Focus especially on AttendanceController and " +
//                             "attendance/mark and attendance/mark-batch endpoints.",

//                     // Employee
//                     "Generate detailed API functional test cases for all employee-related endpoints " +
//                             "in this project. Focus especially on EmployeeController and " +
//                             "/api/employe, /api/employe/{id}, /api/employee, /api/employee/{id} endpoints."
//             );

//             int index = 1;
//             for (String question : questions) {
//                 System.out.println("\nCalling RAG pipeline for question #" + index + ":\n" + question + "\n");

//                 String llmOutput = ragService.generateTestCasesForQuestion(question);
//                 String finalJson = ensureJsonArray(llmOutput);

//                 String fileName = "generated-tests-" + index + ".json";
//                 Path outFile = outputDir.resolve(fileName);

//                 Files.writeString(outFile, finalJson);
//                 System.out.println("✅ Wrote generated test cases to: " + outFile.toAbsolutePath());

//                 index++;
//             }

//             System.out.println("\n🎉 All questions processed, test-case files generated.\n");
//         } catch (Exception e) {
//             System.err.println("❌ Error while generating tests: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }

//     /**
//      * Very tolerant: just grab the first [ ... ] block and return it.
//      * No JSON parsing, so the program will NOT crash because of tiny JSON mistakes.
//      */
//     private static String ensureJsonArray(String raw) {
//         if (raw == null) return "[]";

//         String trimmed = raw.trim();
//         int start = trimmed.indexOf('[');
//         int end = trimmed.lastIndexOf(']');

//         if (start != -1 && end != -1 && start < end) {
//             return trimmed.substring(start, end + 1);
//         }

//         // Fallback: wrap as array with single string element
//         return "[ " + "\"" + trimmed.replace("\"", "\\\"") + "\" ]";
//     }
// }

package com.example.college.tools;

import com.example.college.rag.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class GenerateTestsFromJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            Path projectRoot = Paths.get("").toAbsolutePath();
            Path srcMainJava = projectRoot.resolve("src/main/java");
            Path outputDir = projectRoot.resolve("test-cases");
            Files.createDirectories(outputDir);

            Path outFile = outputDir.resolve("all-tests.json");

            System.out.println("Project root: " + projectRoot);
            System.out.println("Scanning Java sources under: " + srcMainJava);

            RagService ragService = new RagService();
            ragService.ingestJavaSources(srcMainJava);

            // ➤ Add ANY number of questions here
            List<String> questions = List.of(
                "Generate detailed API functional test cases for AttendanceController and all attendance endpoints.",
                "Generate detailed API functional test cases for EmployeeController and all employee endpoints."
                // Add more controllers/questions here
            );

            // Load existing file (or create empty array)
            ArrayNode allTests = readArrayFromFile(outFile);

            for (String question : questions) {
                System.out.println("\n====================================");
                System.out.println("Calling RAG pipeline for question:");
                System.out.println(question);
                System.out.println("====================================\n");

                String llmOutput = ragService.generateTestCasesForQuestion(question);
                ArrayNode newTests = parseArrayFromLLM(llmOutput);

                System.out.println("Appending " + newTests.size() + " test cases...");
                allTests.addAll(newTests);
            }

            // Write final merged array
            String pretty = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(allTests);
            Files.writeString(outFile, pretty);

            System.out.println("\n✅ Successfully wrote test cases to: " + outFile.toAbsolutePath());
            System.out.println("Total test cases in file: " + allTests.size());

        } catch (Exception e) {
            System.err.println("❌ Error while generating test cases: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ArrayNode readArrayFromFile(Path file) {
        ArrayNode result = MAPPER.createArrayNode();
        if (!Files.exists(file)) return result;
        try {
            JsonNode node = MAPPER.readTree(Files.readString(file));
            if (node.isArray()) result.addAll((ArrayNode) node);
        } catch (Exception e) {
            System.err.println("WARNING: Could not parse existing file. Using empty array.");
        }
        return result;
    }

    private static ArrayNode parseArrayFromLLM(String raw) {
        ArrayNode empty = MAPPER.createArrayNode();
        if (raw == null) return empty;

        int s = raw.indexOf('[');
        int e = raw.lastIndexOf(']');
        if (s == -1 || e == -1 || s >= e) {
            System.err.println("WARNING: LLM did not return JSON array.");
            return empty;
        }

        String arr = raw.substring(s, e + 1);

        try {
            JsonNode node = MAPPER.readTree(arr);
            if (node.isArray()) return (ArrayNode) node;
        } catch (Exception ex) {
            System.err.println("WARNING: LLM JSON invalid, skipping. Error: " + ex.getMessage());
        }
        return empty;
    }
}
