package com.example.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Summarizes JUnit Surefire results as:
 * TC_ID: Description: ACC/REJ
 * TOTAL TC: N
 */
public class SummarizeResults {

    // Paths (adjust if your structure is different)
    private static final Path JSON_PATH =
            Paths.get("test-cases", "attendance-functional-tests.json");
    private static final Path SUREFIRE_DIR =
            Paths.get("target", "surefire-reports");

    public static void main(String[] args) {
        try {
            Map<String, String> idToDesc = loadTestCases(JSON_PATH);
            Map<String, String> idToResult = parseJUnitResults(SUREFIRE_DIR);

            int total = idToDesc.size();

            System.out.println("----- FUNCTIONAL TEST SUMMARY -----");
            for (Map.Entry<String, String> entry : idToDesc.entrySet()) {
                String tcId = entry.getKey();
                String desc = entry.getValue();
                String status = idToResult.getOrDefault(tcId, "REJ"); // not executed -> REJ
                System.out.println(tcId + ": " + desc + ": " + status);
            }
            System.out.println("TOTAL TC: " + total);
        } catch (Exception e) {
            System.err.println("[SummarizeResults] ERROR: " + e.getMessage());
            e.printStackTrace();
            // Exit gracefully with code 0 so CI doesn't fail
            System.exit(0);
        }
    }

    /**
     * Load JSON test cases and return a map: Test Case ID -> Description.
     */
    private static Map<String, String> loadTestCases(Path jsonPath) throws IOException {
        if (!Files.exists(jsonPath)) {
            throw new IllegalStateException("Test case JSON file not found: " + jsonPath.toAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> cases = mapper.readValue(
                Files.readAllBytes(jsonPath),
                new TypeReference<List<Map<String, Object>>>() {}
        );

        // Use LinkedHashMap to preserve order
        Map<String, String> idToDesc = new LinkedHashMap<>();
        for (Map<String, Object> c : cases) {
            Object idObj = c.get("Test Case ID");
            Object descObj = c.getOrDefault("Description", "");
            if (idObj != null) {
                idToDesc.put(idObj.toString(), descObj == null ? "" : descObj.toString());
            }
        }
        return idToDesc;
    }

    /**
     * Parse Surefire JUnit XML reports and return map: Test Case ID -> "ACC"/"REJ".
     * A test method is mapped if its name starts with something like "TC_...".
     */
    private static Map<String, String> parseJUnitResults(Path surefireDir) throws Exception {
        Map<String, String> results = new HashMap<>();
        if (!Files.exists(surefireDir)) {
            System.out.println("Surefire directory not found: " + surefireDir.toAbsolutePath());
            return results;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Pattern idPattern = Pattern.compile("(TC_[A-Z0-9_]+).*");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(surefireDir, "TEST-*.xml")) {
            for (Path xmlFile : stream) {
                Document doc = builder.parse(Files.newInputStream(xmlFile));
                doc.getDocumentElement().normalize();

                NodeList testcases = doc.getElementsByTagName("testcase");
                for (int i = 0; i < testcases.getLength(); i++) {
                    Node node = testcases.item(i);
                    if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                    String name = node.getAttributes().getNamedItem("name").getTextContent();
                    Matcher m = idPattern.matcher(name);
                    if (!m.matches()) continue;

                    String tcId = m.group(1);

                    // Check for <failure> or <error> children
                    NodeList children = node.getChildNodes();
                    boolean failed = false;
                    for (int j = 0; j < children.getLength(); j++) {
                        Node child = children.item(j);
                        if (child.getNodeType() != Node.ELEMENT_NODE) continue;
                        String childName = child.getNodeName();
                        if ("failure".equals(childName) || "error".equals(childName)) {
                            failed = true;
                            break;
                        }
                    }

                    if (failed) {
                        results.put(tcId, "REJ");
                    } else {
                        // Only set ACC if not already REJ
                        results.putIfAbsent(tcId, "ACC");
                    }
                }
            }
        }

        return results;
    }
}
