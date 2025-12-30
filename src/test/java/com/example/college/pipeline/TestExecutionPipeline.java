package com.example.college.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Component
public class TestExecutionPipeline {

    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ===============================
    // Execute all test cases
    // ===============================
    public List<TestResult> executeAllTests() throws Exception {

        Path file = Paths.get("test-cases/all-tests.json");
        JsonNode root = MAPPER.readTree(Files.readString(file));

        List<TestResult> results = new ArrayList<>();

        for (JsonNode tc : root) {
            results.add(executeSingleTest(tc));
        }

        return results;
    }

    // ===============================
    // Execute single test case
    // ===============================
    private TestResult executeSingleTest(JsonNode tc) {

        String id = readText(tc, "Test Case ID");
        String title = readText(tc, "Title");

        try {
            ExecutableSpec spec = ApiSpec.from(tc);

            MockHttpServletRequestBuilder request;

            boolean hasPathVars = spec.endpoint.contains("{");


    if (!hasPathVars) {
        // No {id} in URL → no path params needed
        request = switch (spec.method.toUpperCase()) {
            case "POST" -> post(spec.endpoint);
            case "PUT" -> put(spec.endpoint);
            case "DELETE" -> delete(spec.endpoint);
            default -> get(spec.endpoint);
        };
    } else {
        // Endpoint has path variables → must validate pathParams
        if (spec.pathParams == null || spec.pathParams.isEmpty()) {
            return new TestResult(
                id,
                title,
                spec.expectedStatus,
                -1,
                "FAIL",
                "Missing path parameters for endpoint"
            );
        }

        for (Object v : spec.pathParams.values()) {
            if (v == null) {
                return new TestResult(
                    id,
                    title,
                    spec.expectedStatus,
                    -1,
                    "FAIL",
                    "Path variable is null, cannot expand URI"
                );
            }
        }

        Object[] uriVars = spec.pathParams.values().toArray();

        request = switch (spec.method.toUpperCase()) {
            case "POST" -> post(spec.endpoint, uriVars);
            case "PUT" -> put(spec.endpoint, uriVars);
            case "DELETE" -> delete(spec.endpoint, uriVars);
            default -> get(spec.endpoint, uriVars);
        };
    }


            // Query params
            if (spec.queryParams != null) {
                for (Map.Entry<String, String> e : spec.queryParams.entrySet()) {
                    request.param(e.getKey(), e.getValue());
                }
            }

            // Body
            if (spec.body != null) {
                request.contentType(MediaType.APPLICATION_JSON)
                       .content(MAPPER.writeValueAsString(spec.body));
            }

            MvcResult result = mockMvc.perform(request).andReturn();
            int actualStatus = result.getResponse().getStatus();

            return new TestResult(
                id,
                title,
                spec.expectedStatus,
                actualStatus,
                actualStatus == spec.expectedStatus ? "PASS" : "FAIL",
                actualStatus == spec.expectedStatus
                    ? "Executed successfully"
                    : "Expected " + spec.expectedStatus + " but got " + actualStatus
            );

        } catch (Exception e) {
            return new TestResult(
                id,
                title,
                -1,
                -1,
                "FAIL",
                e.getMessage()
            );
        }
    }

    private String readText(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull()
            ? node.get(key).asText()
            : "UNKNOWN";
    }
}
