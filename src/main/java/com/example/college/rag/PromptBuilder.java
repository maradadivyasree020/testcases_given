package com.example.college.rag;

import java.util.List;
import java.util.Map;

public class PromptBuilder {

    public String buildTestGenerationPrompt(String question, List<Map<String, String>> codeSnippets) {
        StringBuilder ctx = new StringBuilder();

        for (Map<String, String> meta : codeSnippets) {
            String file = meta.getOrDefault("file", "UnknownFile");
            String content = meta.getOrDefault("content", "");
            ctx.append("// File: ").append(file).append("\n");
            ctx.append("```java\n").append(content).append("\n```\n\n");
        }

        //for attendance controller
        // return """
        //   You are a senior Java QA engineer.

        //   Based ONLY on the following Java code, generate high quality API test cases.

        //   Requirements:
        //   - Cover positive, negative, boundary, and error scenarios.
        //   - Output a valid JSON ARRAY.
        //   - VERY IMPORTANT: Return ONLY raw JSON. 
        //     No explanations, no comments (// or /* */), no markdown, no ``` fences.
        //   - Each element must have:
        //     "Test Case ID", "Title", "Description", "Pre-Conditions",
        //     "Test Steps", "Input", "Expected Result", "Priority", "Type".
        //   - "Input" should itself be an object with fields relevant to APIs (like endpoint, method, body).
        //   - If some fields are not applicable, use null.

        //   CONTEXT (Java code):
        //   %s

        //   QUESTION:
        //   %s
        // """.formatted(ctx, question);

        //for employee controller
        // return """
        //   You are a senior Java QA engineer.

        //   Based ONLY on the following Java code, generate high quality API test cases
        //   for all Employee-related endpoints.

        //   Requirements:
        //   - Cover positive, negative, boundary, and error scenarios.
        //   - Output a valid JSON ARRAY.
        //   - VERY IMPORTANT: Return ONLY raw JSON.
        //     No explanations, no comments, no markdown, no backticks.
        //   - Each element must contain:
        //     "Test Case ID", "Title", "Description", "Pre-Conditions",
        //     "Test Steps", "Input", "Expected Result", "Priority", "Type".
        //   - "Input" must be an object including fields like:
        //       endpoint, method, pathParams, queryParams, body.
        //   - If something doesn’t apply, use null.

        //   CONTEXT (Java code):
        //   %s

        //   QUESTION:
        //   %s
        // """.formatted(ctx, question);

      //employee controller to eliminate repeat function
    //   return """
    //     You are a senior Java QA engineer.

    //     Based ONLY on the following Java code, generate high quality API test cases
    //     for all Employee-related endpoints.

    //     Requirements:
    //     - Cover positive, negative, boundary, and error scenarios.
    //     - Output a valid JSON ARRAY.
    //     - VERY IMPORTANT: Return ONLY raw JSON.
    //       No explanations, no comments, no markdown, no backticks.
    //     - All values must be plain JSON values (string, number, boolean, null).
    //       Do NOT use code-like expressions such as "A".repeat(255) or concatenation.
    //     - If you need to mention long strings, just use a short placeholder like "MAX_LENGTH_NAME".
    //     - Each element must contain:
    //       "Test Case ID", "Title", "Description", "Pre-Conditions",
    //       "Test Steps", "Input", "Expected Result", "Priority", "Type".
    //     - "Input" must be an object including fields like:
    //         endpoint, method, pathParams, queryParams, body.
    //     - If something doesn’t apply, use null.

    //     CONTEXT (Java code):
    //     %s

    //     QUESTION:
    //     %s
    // """.formatted(ctx, question);

    //genralized prompt
    return """
      You are a senior Java QA engineer.

      Based ONLY on the provided Java code, generate high-quality API functional test cases.

      OUTPUT RULES (VERY IMPORTANT):
      - Output ONLY a valid JSON ARRAY.
      - NO comments, NO markdown, NO text outside the array.
      - Every value must be a plain JSON value (string, number, boolean, null).
      - DO NOT output code constructs like "A".repeat(255) or concatenation.
      - Give as many test cases as possible.
      - If you need long strings, use placeholders like "MAX_NAME", "MAX_ROLE".
      - Each test case object must include:
        "Test Case ID", "Title", "Description", "Pre-Conditions",
        "Test Steps", "Input", "Expected Result", "Priority", "Type".
      - "Input" must contain: endpoint, method, pathParams, queryParams, body.

      CONTEXT (Java code):
      %s

      QUESTION:
    %s
    """.formatted(ctx, question);


    }
}
