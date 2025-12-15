package com.example.college.tools;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import java.nio.file.*;
import java.util.*;

public class TestCaseStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ObjectNode load(Path file) throws Exception {
        if (!Files.exists(file))
            return MAPPER.createObjectNode();
        return (ObjectNode) MAPPER.readTree(Files.readString(file));
    }

    public static void save(Path file, ObjectNode root) throws Exception {
        Files.writeString(file,
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    }

    public static ArrayNode getTests(ObjectNode root, String endpointId) {
        return root.has(endpointId)
                ? (ArrayNode) root.get(endpointId).get("testCases")
                : null;
    }
}
