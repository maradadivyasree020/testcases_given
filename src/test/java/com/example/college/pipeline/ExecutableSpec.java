package com.example.college.pipeline;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class ExecutableSpec {

    public String method;
    public String endpoint;
    public JsonNode body;
    public Map<String, String> queryParams;
    public Map<String,Object> pathParams;
    public int expectedStatus;

    public ExecutableSpec(String method,
                          String endpoint,
                          JsonNode body,
                          Map<String, String> queryParams,
                          Map<String, Object> pathParams,
                          int expectedStatus) {
        this.method = method;
        this.endpoint = endpoint;
        this.body = body;
        this.queryParams = queryParams;
        this.pathParams = pathParams;
        this.expectedStatus = expectedStatus;
    }
}
