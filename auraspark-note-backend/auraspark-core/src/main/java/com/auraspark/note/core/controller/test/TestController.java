package com.auraspark.note.core.controller.test;

import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.core.entity.TestUser;
import com.auraspark.note.core.service.test.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Test", description = "Health check and database connection test")
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @Operation(summary = "Health check")
    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        return ApiResponse.success("Auraspark is running!");
    }

    @Operation(summary = "Database connection test")
    @GetMapping("/db")
    public ApiResponse<List<TestUser>> testDatabase() {
        List<TestUser> users = testService.getTestUser();
        return ApiResponse.success(users, "Database connected successfully", "test.db.connected");
    }
}
