package com.example.college.tools;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class EndpointExtractor {

    public static class Endpoint {
        public String id;       // PUT_/api/attendance/mark
        public String code;     // full method code
    }

    public static List<Endpoint> extract(Path controllerFile) throws Exception {
        String src = Files.readString(controllerFile);

        Pattern p = Pattern.compile(
                "@(Get|Post|Put|Delete)Mapping\\(\"([^\"]+)\"\\)[\\s\\S]*?\\{[\\s\\S]*?\\n\\}",
                Pattern.MULTILINE);

        Matcher m = p.matcher(src);
        List<Endpoint> endpoints = new ArrayList<>();

        while (m.find()) {
            String method = m.group(1).toUpperCase();
            String path = m.group(2);
            String code = m.group();

            Endpoint e = new Endpoint();
            e.id = method + "_" + resolveBasePath(src) + path;
            e.code = code;
            endpoints.add(e);
        }
        return endpoints;
    }

    private static String resolveBasePath(String src) {
        Matcher m = Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)").matcher(src);
        return m.find() ? m.group(1) : "";
    }
}
