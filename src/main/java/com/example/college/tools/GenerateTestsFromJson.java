package com.example.college.tools;

import com.example.college.rag.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class GenerateTestsFromJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ArrayNode DIFF_REPORT = MAPPER.createArrayNode();

    public static void main(String[] args) {
        try {
            Path root = Paths.get("").toAbsolutePath();
            Path src = root.resolve("src/main/java");
            Path outDir = root.resolve("test-cases");
            Path testdataPath = root.resolve("resources");
            Files.createDirectories(outDir);

            Path testFile = outDir.resolve("all-tests.json");
            Path metaFile = outDir.resolve("test-metadata.json");
            Path attendanceTestData=outDir.resolve("testdata/attendance-testdata.xlsx");

            RagService rag = new RagService();
            rag.ingestJavaSources(src);

            ArrayNode allTests = readArray(testFile);
            ObjectNode meta = readObject(metaFile);

            String runId = Instant.now().toString();

            // Extract endpoints
            Map<String, EndpointExtractor.EndpointInfo> endpoints = EndpointExtractor.extractEndpoints(src);

            for (EndpointExtractor.EndpointInfo ep : endpoints.values()) {
                String epKey = ep.endpointKey;       // GET:/employee/{id}
                // String epCode = ep.getValue();

                // String newHash = EndpointHasher.hash(epCode);
                String newHash = computeEndpointHash(src,ep);
                String oldHash = meta.path(epKey).asText(null);

                ArrayNode oldTests = filterTests(allTests, epKey);

                if (oldHash == null) {
                    System.out.println("\n[NEW ENDPOINT] " + epKey);

                    //TODO : NOT PASSING TEST DATA
                    ArrayNode gen = generate(rag, epKey, null);
                    assignStableIds(gen, epKey);
                    tag(gen, epKey, newHash, runId, "GENERATE");

                    printDiff(epKey, MAPPER.createArrayNode(), gen);

                    allTests.addAll(gen);
                }
                else if (!oldHash.equals(newHash)) {
                    System.out.println("\n[MODIFIED ENDPOINT] " + epKey);

                    ArrayNode edited = edit(rag, oldTests, epKey);
                    assignStableIds(edited, epKey);
                    tag(edited, epKey, newHash, runId, "EDIT");

                    printDiff(epKey, oldTests, edited);

                    //TODO: STUDY MORE ABOUT THIS
                    removeOld(allTests, epKey);
                    allTests.addAll(edited);
                }
                else {
                    System.out.println("[SKIP] " + epKey);
                }

                meta.put(epKey, newHash);
            }

            Files.writeString(testFile,MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(allTests));

            Files.writeString(metaFile,MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(meta));

            System.out.println("\n DONE");

            Path diffFile = outDir.resolve("diff.json");

            Files.writeString(diffFile,MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(DIFF_REPORT),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("diff.json written with "+ DIFF_REPORT.size() + " change(s)");

            ArrayNode updatedDiff = buildUpdatedDiff(DIFF_REPORT);
            Path updatedDiffFile = outDir.resolve("updated-diff.json");
            if (DIFF_REPORT.size() > 0) {
                Files.writeString(updatedDiffFile,MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(updatedDiff));
                System.out.println("updated-diff.json written with "+ updatedDiff.size() + " entry(s)");
            }
            else {
                Files.writeString(updatedDiffFile,"[]",StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println(updatedDiff.size()+"updated-diff.json cleared (no changes)");
            } 
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- HELPERS ----------------

    static JsonNode stripMeta(JsonNode tc) {
        ObjectNode copy = tc.deepCopy();
        copy.remove("_meta");
        return copy;
    }

    static ArrayNode generate(RagService rag, String ep, String data) {
        String out = rag.generateTestCasesForQuestion(ep, data);
        return parse(out);
    }

    static ArrayNode edit(RagService rag, ArrayNode old, String ep) {
        String out = rag.editExistingTests(old.toString(), ep, null);
        return parse(out);
    }

    // Stable IDs: endpoint + index
    static void assignStableIds(ArrayNode arr, String ep) {
        int i = 1;
        for (JsonNode n : arr) {
            ((ObjectNode) n).put(
                "Test Case ID",
                ep.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase()
                    + "-" + String.format("%03d", i++)
            );
        }
    }

    static void tag(ArrayNode arr, String ep, String hash, String runId, String mode) {
        for (JsonNode n : arr) {
            ObjectNode m = MAPPER.createObjectNode();
            m.put("endpoint", ep);
            m.put("hash", hash);
            m.put("runId", runId);
            m.put("mode", mode);
            ((ObjectNode) n).set("_meta", m);
        }
    }

    static void removeOld(ArrayNode all, String ep) {
        ArrayNode keep = MAPPER.createArrayNode();
        for (JsonNode t : all) {
            if (!ep.equals(t.path("_meta").path("endpoint").asText())) {
                keep.add(t);
            }
        }
        all.removeAll();
        all.addAll(keep);
    }

    static ArrayNode filterTests(ArrayNode all, String ep) {
        ArrayNode out = MAPPER.createArrayNode();
        for (JsonNode t : all) {
            if (ep.equals(t.path("_meta").path("endpoint").asText())) {
                out.add(t);
            }
        }
        return out;
    }

    static void printDiff(String ep, ArrayNode oldTests, ArrayNode newTests) {
        Map<String, JsonNode> oldMap = mapById(oldTests);
        Map<String, JsonNode> newMap = mapById(newTests);

        System.out.println("TEST CASE CHANGES FOR " + ep);

        boolean changed = false;

        for (String id : newMap.keySet()) {
            if (!oldMap.containsKey(id)) {
                System.out.println("\nNEW : " + id);
                System.out.println(pretty(newMap.get(id)));
                
                recordDiff(ep, "NEW", id, null, newMap.get(id));
                changed = true;
            } 
            else if (!oldMap.get(id).equals(newMap.get(id))) {
                System.out.println("\nMODIFIED : " + id);

                System.out.println("----- OLD -----");
                System.out.println(pretty(oldMap.get(id)));

                System.out.println("----- NEW -----");
                System.out.println(pretty(newMap.get(id)));

                recordDiff(ep, "MODIFIED", id,oldMap.get(id),newMap.get(id));
                changed = true;
            }
        }
        //REMOVED TC
        for (String id : oldMap.keySet()) {
            if (!newMap.containsKey(id)) {
                System.out.println("\nREMOVED : " + id);
                System.out.println(pretty(oldMap.get(id)));

                recordDiff(ep, "REMOVED", id,oldMap.get(id),null);
                changed = true;
            }
        }
        if (!changed) {
            System.out.println("No test case changes");
        }
    }

    static ArrayNode buildUpdatedDiff(ArrayNode diffJson) {

        ArrayNode updated = MAPPER.createArrayNode();

        for (JsonNode d : diffJson) {

            JsonNode oldTc = d.get("old");
            JsonNode newTc = d.get("new");

        // NEW or REMOVED → keep as-is
            if (oldTc == null || newTc == null) {
                updated.add(d);
                continue;
            }

            JsonNode oldClean = stripMeta(oldTc);
            JsonNode newClean = stripMeta(newTc);

            boolean realChange = !oldClean.equals(newClean);

            ObjectNode out = MAPPER.createObjectNode();
            out.put("endpoint", d.get("endpoint").asText());
            out.put("testCaseId", d.get("testCaseId").asText());

            if (realChange) {
            // ✅ real change → keep both
                out.put("changeType", "CONTENT_CHANGED");
                out.set("old", oldTc);
                out.set("new", newTc);
            } 
            else {
                // ❌ only metadata change → keep OLD only
                out.put("changeType", "SERVICE_ONLY_CHANGE");
                out.set("old", oldTc);
            }

            updated.add(out);
        }
    return updated;
}



    static Map<String, JsonNode> mapById(ArrayNode arr) {
        Map<String, JsonNode> map = new HashMap<>();
        for (JsonNode t : arr) {
            map.put(t.get("Test Case ID").asText(), t);
        }
        return map;
    }

    static ArrayNode readArray(Path p) throws Exception {
        if (!Files.exists(p)) return MAPPER.createArrayNode();
        return (ArrayNode) MAPPER.readTree(Files.readString(p));
    }

    static ObjectNode readObject(Path p) throws Exception {
        if (!Files.exists(p)) return MAPPER.createObjectNode();
        return (ObjectNode) MAPPER.readTree(Files.readString(p));
    }

    private static ArrayNode parse(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return MAPPER.createArrayNode();
            }

            int start = raw.indexOf('[');
            int end   = raw.lastIndexOf(']');

            if (start < 0 || end <= start) {
                throw new RuntimeException("LLM output does not contain a JSON array");
            }

            String json = raw.substring(start, end + 1);
            JsonNode node = MAPPER.readTree(json);

            if (!node.isArray()) {
                throw new RuntimeException("Parsed JSON is not an array");
            }

            return (ArrayNode) node;

        } catch (Exception e) {
            System.err.println("Failed to parse LLM output:");
            System.err.println(raw);
            throw new RuntimeException("Test case parsing failed", e);
        }
    }

    static String computeEndpointHash(Path srcRoot,EndpointExtractor.EndpointInfo ep) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Controller method
            digest.update(ep.controllerCode.getBytes());

            // Dependencies (service / repo / model)
            for (String dep : ep.dependencies) {
                Path p = findClassFile(srcRoot, dep);
                if (p != null && Files.exists(p)) {
                    digest.update(Files.readAllBytes(p));
                }
            }

            //raw hashbytes-hexcodea
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Returns .java files
    static Path findClassFile(Path srcRoot, String className) throws Exception {
        try (var stream = Files.walk(srcRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(className + ".java"))
                    .findFirst()
                    .orElse(null);
        }
    }

    static String pretty(JsonNode node) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(node);
        } catch (Exception e) {
            return node.toString();
        }
    }

    static void recordDiff(String ep,String changeType,String testCaseId,JsonNode oldTc,JsonNode newTc) {
        ObjectNode d = DIFF_REPORT.addObject();
        d.put("endpoint", ep);
        d.put("changeType", changeType);
        d.put("testCaseId", testCaseId);

    if (oldTc != null) {
        d.set("old", oldTc);
    }
    if (newTc != null) {
        d.set("new", newTc);
    }
}
}
