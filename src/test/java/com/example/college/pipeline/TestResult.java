package com.example.college.pipeline;

public class TestResult {

    public String testCaseId;
    public String title;
    public int expectedStatus;
    public int actualStatus;
    public String status;
    public String message;

    public TestResult(String testCaseId,
                      String title,
                      int expectedStatus,
                      int actualStatus,
                      String status,
                      String message) {

        this.testCaseId = testCaseId;
        this.title = title;
        this.expectedStatus = expectedStatus;
        this.actualStatus = actualStatus;
        this.status = status;
        this.message = message;
    }
}
