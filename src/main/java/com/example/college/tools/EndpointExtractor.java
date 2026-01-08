package com.example.college.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointExtractor {

    // Matches @GetMapping("/x"), @PostMapping, @PutMapping, @DeleteMapping
    private static final Pattern MAPPING_PATTERN = Pattern.compile(
            "@(Get|Post|Put|Delete)Mapping\\s*\\(\\s*\"([^\"]+)\"\\s*\\)"
    );

    // Matches method declaration
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(public|private|protected)\\s+[^{]+\\{",
            Pattern.MULTILINE
    );

    private static final Set<String> IGNORED_PATHS = loadIgnoredPaths();

    private static Set<String> loadIgnoredPaths() {
        Properties p = loadProps("config/dependencies.properties");
        String v = p.getProperty("paths", "");

        Set<String> set = new HashSet<>();
        for (String s : v.split(",")) {
            set.add(s.trim());   
        }
        return set;
    }


    public static Map<String, EndpointInfo> extractEndpoints(Path srcRoot) throws IOException {
        Map<String, EndpointInfo> endpoints = new LinkedHashMap<>();

        Files.walk(srcRoot)
                .filter(p -> p.toString().endsWith("Controller.java"))
                .forEach(file -> {
                    try {
                        String code = Files.readString(file);
                        extractFromController(code, endpoints);
                    } catch (Exception e) {
                        System.err.println("Failed parsing " + file);
                        e.printStackTrace();
                    }
                });

        return endpoints;
    }

    private static void extractFromController(String code,Map<String, EndpointInfo> out) {
        Matcher m = MAPPING_PATTERN.matcher(code);

        while (m.find()) {
            String httpMethod = m.group(1).toUpperCase();
            String path = m.group(2);
            // if (path.equals("/run") || path.equals("/diff") || path.equals("/all") || path.equals("/prompt")) {
            //     continue;
            // }
            if (IGNORED_PATHS.contains(path))
            continue;

            String endpointKey = httpMethod + ":" + path;

            int searchFrom = m.end();
            Matcher methodMatcher = METHOD_PATTERN.matcher(code.substring(searchFrom));
            if (!methodMatcher.find()) continue;

            int methodStart = searchFrom + methodMatcher.start();
            int bodyStart   = searchFrom + methodMatcher.end() - 1;

            // ---- Extract full method body ----
            int braceCount = 1;
            int i = bodyStart + 1;
            while (i < code.length() && braceCount > 0) {
                if (code.charAt(i) == '{') braceCount++;
                else if (code.charAt(i) == '}') braceCount--;
                i++;
            }

            String methodCode = code.substring(m.start(), i);

            // ---- Extract dependencies from method body ----
            // Set<String> deps = extractDependencies(methodCode);
            Set<String> deps = extractDependencies(code); // full controller code

            out.put(
                endpointKey,
                new EndpointInfo(endpointKey, methodCode, deps)
            );
        }
    }

    private static Properties loadProps(String path) {
        try (InputStream is =
            EndpointExtractor.class
                .getClassLoader()
                .getResourceAsStream(path)) {

            if (is == null)
                throw new RuntimeException("Missing config: " + path);

            Properties p = new Properties();
            p.load(is);
            return p;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // private static Set<String> extractDependencies(String controllerCode) {
    //     Set<String> deps = new HashSet<>();

    //     // --- Service fields ---
    //     if (controllerCode.matches("(?s).*AttendanceService\\s+\\w+.*"))
    //         deps.add("AttendanceService");

    //     if (controllerCode.matches("(?s).*EmployeeService\\s+\\w+.*"))
    //         deps.add("EmployeeService");

    //     // --- Repo fields ---
    //     if (controllerCode.matches("(?s).*AttendanceRepo\\s+\\w+.*"))
    //         deps.add("AttendanceRepo");

    //     if (controllerCode.matches("(?s).*EmployeeRepo\\s+\\w+.*"))
    //         deps.add("EmployeeRepo");

    //     // --- Models ---
    //     if (controllerCode.contains("AttendanceModel"))
    //         deps.add("AttendanceModel");

    //     if (controllerCode.contains("EmployeeModel"))
    //         deps.add("EmployeeModel");

    //     return deps;
    // }

    private static final Map<String, Set<String>> DEPENDENCIES = loadDependencies();

    private static Map<String, Set<String>> loadDependencies() {
        Properties p = loadProps("config/dependencies.properties");
        Map<String, Set<String>> map = new HashMap<>();

        for (String key : p.stringPropertyNames()) {
            map.put(key, Set.of(p.getProperty(key).split(",")));
        }
        return map;
    }

    private static Set<String> extractDependencies(String code) {
        Set<String> out = new HashSet<>();
        DEPENDENCIES.values().forEach(set ->
            set.forEach(d -> { if (code.contains(d)) out.add(d); })
        );
        return out;
    }


    // ===============================
    // DATA HOLDER
    // ===============================
    public static class EndpointInfo {
        public final String endpointKey;
        public final String controllerCode;
        public final Set<String> dependencies;

        public EndpointInfo(
                String endpointKey,
                String controllerCode,
                Set<String> dependencies
        ) {
            this.endpointKey = endpointKey;
            this.controllerCode = controllerCode;
            this.dependencies = dependencies;
        }
    }
}
