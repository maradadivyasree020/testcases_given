package com.example.college.rag;

import java.util.List;
import java.util.Map;

public class PromptBuilder {

    /**
     * OLD MODE (no explicit test data) – still used by simpler callers.
     * Builds context from snippet metadata and delegates to the main builder.
     */
    public String buildTestGenerationPrompt(String question, List<Map<String, String>> snippetMaps) {
        StringBuilder ctx = new StringBuilder();
        for (Map<String, String> m : snippetMaps) {
            ctx.append("// File: ")
              .append(m.getOrDefault("file", "UnknownFile"))
              .append("\n");
            ctx.append(m.getOrDefault("content", ""))
              .append("\n\n");
        }
        // no extra test data
        return buildTestGenerationPrompt(ctx.toString(), question, null);
    }

    /**
     * NEW MODE – full control with:
     *  - ctx (java code chunks)
     *  - question
     *  - extraTestData (JSON or text – e.g., from Excel)
     */
    public String buildTestGenerationPrompt(String ctx, String question, String extraTestData) {

        String testDataSection = (extraTestData == null || extraTestData.isBlank())
                ? "No explicit test data provided. You may choose reasonable values."
                : extraTestData;

        return """
            You are a senior Java QA engineer.

            Based ONLY on the provided Java code AND the test data below,
            generate high-quality API FUNCTIONAL test cases.

            TEST DATA (use these values for request bodies whenever possible):
            %s

            OUTPUT RULES (VERY IMPORTANT):
            - Output ONLY a valid JSON ARRAY.
            - NO comments, NO markdown, NO backticks, NO extra text.
            - Every value must be a plain JSON value (string, number, boolean, null).
            - DO NOT output code constructs like "A".repeat(255), concatenation, or method calls.
            - Prefer using the values from TEST DATA in Input.body.

            EACH TEST CASE OBJECT MUST CONTAIN:
            - "Test Case ID"
            - "Title"
            - "Description"
            - "Pre-Conditions"
            - "Test Steps"   (array of strings)
            - "Input"         (object: endpoint, method, pathParams, queryParams, body)
            - "Expected Result"
            - "Priority"     (High / Medium / Low)
            - "Type"         (Positive / Negative / Boundary / Error)

            CONTEXT (Java code):
            %s

            QUESTION:
            %s
            """.formatted(testDataSection, ctx, question);
    }
    public String buildEditPrompt(
        String oldTestsJson,
        String ctx,
        String testDataJson,
        String question
        ) {
            return """
            You are EDITING existing API test cases.

            STABILITY RULES (MANDATORY):
            - DO NOT change Test Case ID
            - DO NOT reword Title or Description unless incorrect
            - DO NOT change Steps unless logic is broken
            - DO NOT reformat JSON
            - Only update fields that are logically invalid due to code change
            - If a test case is still valid, return it EXACTLY as-is

            OLD TEST CASES:
            %s

            CONTROLLER CODE:
            %s

            TEST DATA:
            %s

            QUESTION:
            %s

            OUTPUT:
            Return ONLY the updated JSON array.
            """
            .formatted(oldTestsJson, ctx, testDataJson, question);
        }

}
