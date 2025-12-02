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

        // return """
        //     You are a senior Java QA engineer.

        //     Based ONLY on the following Java code, generate high quality API test cases.

        //     Requirements:
        //     - Cover positive, negative, boundary, and error scenarios.
        //     - Output JSON array. Each element must have:
        //       "Test Case ID", "Title", "Description", "Pre-Conditions",
        //       "Test Steps", "Input", "Expected Result", "Priority", "Type".
        //     - "Input" should itself be an object with fields relevant to APIs (like endpoint, body).
        //     - If some fields are not applicable, use null.

        //     CONTEXT (Java code):
        //     %s

        //     QUESTION:
        //     %s
        //     """.formatted(ctx, question);

        return """
    You are a senior Java QA engineer.

    Based ONLY on the following Java code, generate high quality API test cases.

    Requirements:
    - Cover positive, negative, boundary, and error scenarios.
    - Output a valid JSON ARRAY.
    - VERY IMPORTANT: Return ONLY raw JSON. 
      No explanations, no comments (// or /* */), no markdown, no ``` fences.
    - Each element must have:
      "Test Case ID", "Title", "Description", "Pre-Conditions",
      "Test Steps", "Input", "Expected Result", "Priority", "Type".
    - "Input" should itself be an object with fields relevant to APIs (like endpoint, method, body).
    - If some fields are not applicable, use null.

    CONTEXT (Java code):
    %s

    QUESTION:
    %s
    """.formatted(ctx, question);


    }
}
