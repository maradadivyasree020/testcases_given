package com.example.college.pipeline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.example.college.CollegeApplication;
import com.example.college.controller.AttendanceController;
import com.example.college.controller.EmployeeController;
import com.example.college.controller.TestCaseController;
import com.example.college.service.AttendanceService;


@WebMvcTest(controllers = {AttendanceController.class,EmployeeController.class,TestCaseController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JsonDrivenTestRunnerTest {

    @Autowired
    private TestExecutionPipeline pipeline;

    @MockBean
    private AttendanceService attendanceService;

    @Test
    void runJsonDrivenTestsAndGeneratePdf() throws Exception {
        var results = pipeline.executeAllTests();
        PdfReportGenerator.generate(results);
    }
}
