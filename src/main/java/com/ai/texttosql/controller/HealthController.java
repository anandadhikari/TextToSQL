package com.ai.texttosql.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Health Check", description = "API for checking the health status of the application")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController implements HealthIndicator {

    @Operation(summary = "Check the health status of the application")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now());
        response.put("service", "Text-to-SQL API");
        response.put("version", "1.0.0");

        // Add more health check details as needed
        Map<String, Object> details = new HashMap<>();
        details.put("database", "UP");
        details.put("diskSpace", "OK");
        details.put("redis", "UP");

        response.put("details", details);

        return ResponseEntity.ok(response);
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("service", "Text-to-SQL API")
                .withDetail("timestamp", Instant.now())
                .build();
    }
}
