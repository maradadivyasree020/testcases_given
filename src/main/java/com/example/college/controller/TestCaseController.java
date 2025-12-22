package com.example.college.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;


@RestController
@RequestMapping("/api/testcases")
@CrossOrigin(origins = "http://localhost:5173")
public class TestCaseController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Path root() {
        return Paths.get("").toAbsolutePath();
    }

    // ===============================
    // SHOW ALL TC → all-tests.json
    // ===============================
    @GetMapping("/all")
    public ResponseEntity<?> getAllTestCases() {
        try {
            Path file = root().resolve("test-cases/all-tests.json");
            if (!Files.exists(file))
                return ResponseEntity.status(404).body("all-tests.json not found");

            return ResponseEntity.ok(
                    MAPPER.readTree(Files.readString(file))
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // ===============================
    // SHOW OLD VS NEW → diff.json
    // ===============================
    @GetMapping("/diff")
    public ResponseEntity<?> getDiff() {
        try {
            Path file = root().resolve("test-cases/diff.json");
            if (!Files.exists(file))
                return ResponseEntity.status(404).body("diff.json not found");

            return ResponseEntity.ok(
                    MAPPER.readTree(Files.readString(file))
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // ===============================
    // RUN TC → mvn exec + logs
    // ===============================
    @PostMapping("/run")
    public ResponseEntity<?> runTC() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "mvn", "-q", "-DskipTests", "exec:java"
            );

            pb.directory(root().toFile());
            pb.redirectErrorStream(true);

            Process p = pb.start();

            // StringBuilder logs = new StringBuilder();
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                System.out.println("[MAVEN] " + line);
                }
            }

            p.waitFor();
        return ResponseEntity.ok("Generation completed");
        } catch (Exception e) {
        e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
