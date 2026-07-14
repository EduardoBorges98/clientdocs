package com.eduardo.clientdocs.controller;

import com.eduardo.clientdocs.dto.ApiInfoResponse;
import com.eduardo.clientdocs.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ApiInfoResponse apiInfo() {
        return new ApiInfoResponse(
                "ClientDocs Processor",
                "running",
                "1.0.0",
                "/health",
                "/swagger-ui/index.html"
        );
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                "clientdocs-api"
        );
    }
}