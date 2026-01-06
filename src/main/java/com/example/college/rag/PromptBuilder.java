package com.example.college.rag;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

// import com.itextpdf.text.pdf.parser.Path;

public class PromptBuilder {

    private static volatile String LAST_PROMPT;

    public static String getLastPrompt() {
        System.out.println(LAST_PROMPT);
        return LAST_PROMPT;
    }

    public String buildGeneratePrompt(String ctx,String question,String testDataJson) {

        String testDataSection =(testDataJson == null || testDataJson.isBlank())? "No explicit test data provided.":testDataJson;

        String prompt = """
            You are a senior Java QA engineer.

            TASK:
            Generate API FUNCTIONAL test cases.

            IMPORTANT STABILITY RULES:
            - Use CLEAR, DIRECT, NON-CREATIVE language.
            - Avoid paraphrasing or fancy wording.
            - Keep Titles and Descriptions simple and consistent.
            - Prefer deterministic wording (same wording every time).

            IMPORTANT:
            - DO NOT generate or change "Test Case ID"
            - Leave "Test Case ID" as empty string ""

            TEST DATA (use these values strictly where applicable):
            %s

            OUTPUT RULES (STRICT):
            - Output ONLY a valid JSON ARRAY.
            - No markdown, no comments, no explanations.
            - No code constructs (e.g. ".repeat()", concatenation).
            - Every value must be valid JSON.
            - Follow the SAME wording style across all test cases.
            - Expected result write according to the code if error message is given then use that.

            EACH TEST CASE MUST CONTAIN:
            - "Test Case ID"
            - "Controller Name"
            - "Title"
            - "Description"
            - "Pre-Conditions"
            - "Test Steps" (array of strings)
            - "Input" (object: endpoint, method, pathParams, queryParams, body)
            - "Expected Result"
            - "Expected Status"
            - "Priority"
            - "Type"

            CONTROLLER CODE:
            %s

            QUESTION:
            %s
            """;
            try {
                Path TEST_CASES_DIR =
                    Paths.get("").toAbsolutePath().resolve("test-cases");

                Path promptFile = TEST_CASES_DIR.resolve("last-prompt.txt");

                System.out.println("Writing prompt to: " + promptFile.toAbsolutePath());

                Files.writeString(
                    promptFile,
                    prompt,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
            } 
            catch (IOException e) {
                throw new RuntimeException("Failed to write prompt file", e);
            }
            return prompt.formatted(testDataSection, ctx, question);
    }

    public String buildEditPrompt(String oldTestsJson,String ctx,String testDataJson,String question) {

        String prompt = """
            CRITICAL RULE (MUST FOLLOW):
            - Test Case ID defines the scenario and MUST NOT change its intent.
            - Do NOT swap meanings across Test Case IDs.
            - If a Test Case ID already exists:
            - Keep the same logical scenario (inputs, intent).
            - Only update Expected Result if controller behavior truly changed.
            - NEVER flip boolean meanings (true ↔ false) for the same Test Case ID.

                        You are EDITING existing API test cases.

            STABILITY RULES (MANDATORY):
            - DO NOT change "Test Case ID"
            - DO NOT reword "Title"
            - DO NOT reword "Description"
            - DO NOT reorder fields
            - DO NOT remove valid test cases
            - ONLY update a test case if it is logically invalid due to code change
            - If a test case is still valid, return it EXACTLY as-is

            IMPORTANT:
            - DO NOT generate or change "Test Case ID"
            - Leave "Test Case ID" as empty string ""

            OLD TEST CASES (SOURCE OF TRUTH):
            %s

            CONTROLLER CODE (UPDATED):
            %s

            TEST DATA (may be reused):
            %s

            QUESTION:
            %s

            EACH TEST CASE MUST CONTAIN:
            - "Test Case ID"
            - "Controller Name"
            - "Title"
            - "Description"
            - "Pre-Conditions"
            - "Test Steps" (array of strings)
            - "Input" (object: endpoint, method, pathParams, queryParams, body)
            - "Expected Result"
            - "Expected Status"
            - "Priority"
            - "Type"
            
            OUTPUT RULES:
            - Return ONLY a JSON ARRAY
            - Preserve original formatting as much as possible
            - Do NOT add commentary or explanations
            - Expected result write according to the code if error message is given then use that.
            """;
            try {
                Path TEST_CASES_DIR =Paths.get("").toAbsolutePath().resolve("test-cases");

                Path promptFile = TEST_CASES_DIR.resolve("last-prompt.txt");

                System.out.println("Writing prompt to: " + promptFile.toAbsolutePath());

                Files.writeString(
                    promptFile,
                    prompt,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                );
            } 
            catch (IOException e) {
                throw new RuntimeException("Failed to write prompt file", e);
            }
            return prompt.formatted(oldTestsJson, ctx, testDataJson, question);
    }
}
