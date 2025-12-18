package com.example.college.tools;

import java.security.MessageDigest;

public class EndpointHasher {

    public static String hash(String code) {
        try {
            String normalized = normalize(code);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Endpoint hashing failed", e);
        }
    }

    // Remove noise that shouldn't trigger regeneration
    private static String normalize(String s) {
        return s
                .replaceAll("//.*", "")          // remove comments
                .replaceAll("/\\*(.|\\R)*?\\*/", "")
                .replaceAll("\\s+", " ")         // normalize spaces
                .trim();
    }
}
