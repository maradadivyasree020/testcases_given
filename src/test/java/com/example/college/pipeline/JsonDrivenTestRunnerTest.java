package com.example.college.pipeline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class JsonDrivenTestRunnerTest {

    @Autowired
    private TestExecutionPipeline pipeline;

    @Test
    void runJsonDrivenTestsAndGeneratePdf() throws Exception {
        var results = pipeline.executeAllTests();
        PdfReportGenerator.generate(results);
    }
}
