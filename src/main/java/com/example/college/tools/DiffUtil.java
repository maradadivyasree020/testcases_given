package com.example.college.tools;

import com.fasterxml.jackson.databind.JsonNode;

public class DiffUtil {

    public static boolean isBehaviorChanged(JsonNode oldTc, JsonNode newTc) {
        return !same(oldTc.path("Input"), newTc.path("Input")) ||
               !same(oldTc.path("Expected"), newTc.path("Expected"));
    }

    private static boolean same(JsonNode a, JsonNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
