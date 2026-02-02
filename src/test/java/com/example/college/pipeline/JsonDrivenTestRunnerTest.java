package com.example.college.pipeline;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest
@ActiveProfiles("test")                 
@AutoConfigureMockMvc(addFilters = false) 
class JsonDrivenTestRunnerTest {

    @Autowired
    private TestExecutionPipeline pipeline;

    @Test
    // void runJsonDrivenTestsAndGeneratePdf() throws Exception {
    //     var results = pipeline.executeAllTests();
    //     PdfReportGenerator.generate(results);
    // }

    public void runJsonDrivenTestsAndGeneratePdf() throws Exception {
        List<TestResult> results = pipeline.executeAllTests();
        PdfReportGenerator.generate(results);
        Path out = Paths.get("test-cases/test-results.json");
        // new ObjectMapper().writeValue(out.toFile(), results);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out.toFile(), results);

    }
}
