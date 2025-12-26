package com.example.college.rag;

import java.util.ArrayList;
import java.util.List;

public class CodeChunker {

    private final int maxLinesPerChunk;

    public CodeChunker(int maxLinesPerChunk) {
        this.maxLinesPerChunk = maxLinesPerChunk;
    }

    public List<String> chunk(String content) {
        String[] lines = content.split("\\R");
        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        int count = 0;

        for (String line : lines) {
            current.append(line).append(System.lineSeparator());
            count++;
            if (count >= maxLinesPerChunk) {
                chunks.add(current.toString());
                current.setLength(0);
                count = 0;
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
