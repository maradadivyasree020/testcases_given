package com.example.college.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiSpec {

    public static ExecutableSpec from(JsonNode tc) {

        JsonNode input = tc.get("Input");
        if (input == null || input.isNull()) {
            throw new IllegalArgumentException("Missing Input block");
        }

        String method = getText(input, "method");
        String endpoint = getText(input, "endpoint");

        if (method == null || endpoint == null) {
            throw new IllegalArgumentException("Input.method or Input.endpoint missing");
        }

        // Body
        Object body = null;
        if (input.has("body") && !input.get("body").isNull()) {
            body = input.get("body");
        }

        // Query params
        Map<String, String> queryParams = new HashMap<>();
        if (input.has("queryParams") && input.get("queryParams").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = input.get("queryParams").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                queryParams.put(e.getKey(), e.getValue().asText());
            }
        }

        Map<String, Object> pathParams = new LinkedHashMap<>();
        JsonNode pathNode = input.path("pathParams");

        if (pathNode.isObject()) {
            pathNode.fields().forEachRemaining(e -> {
                if (!e.getValue().isNull()) {
                    pathParams.put(e.getKey(), e.getValue().asText());
                }
            });
        }

        int expectedStatus = inferExpectedStatus(tc);

        return new ExecutableSpec(
            method,
            endpoint,
            body,
            queryParams.isEmpty() ? null : queryParams,
            pathParams.isEmpty() ? null : pathParams,
            expectedStatus
        );
    }

    private static String getText(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull()
            ? node.get(key).asText()
            : null;
    }

    private static int inferExpectedStatus(JsonNode tc) {
        if (tc.has("ExpectedStatus")) {
            return tc.get("ExpectedStatus").asInt();
        }

        if (tc.has("Expected Result")) {
            String s = tc.get("Expected Result").asText();
            if (s.contains("200")) return 200;
            if (s.contains("400")) return 400;
            if (s.contains("404")) return 404;
            if (s.contains("500")) return 500;
        }
        return 200;
    }
}
