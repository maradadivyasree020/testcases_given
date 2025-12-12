package com.example.college.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;

@RestController
@RequestMapping("/api")
public class TestCaseController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping("/testcases")
    public ResponseEntity<?> getTestCases() {
        try {
            // Project root (same folder as pom.xml)
            Path projectRoot = Paths.get("").toAbsolutePath();

            // Adjust file name based on what your generator writes
            Path file = projectRoot.resolve("test-cases/final-testcases.json");
            if (!Files.exists(file)) {
                // fallback if you still use all-tests.json
                file = projectRoot.resolve("test-cases/all-tests.json");
            }

            if (!Files.exists(file)) {
                // No file at all -> 404 with message
                return ResponseEntity.status(404)
                        .body("Test cases JSON not found. Run mvn -q -DskipTests exec:java first.");
            }

            String json = Files.readString(file);
            JsonNode node = MAPPER.readTree(json);
            return ResponseEntity.ok(node);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error reading test cases: " + e.getMessage());
        }
    }
}
