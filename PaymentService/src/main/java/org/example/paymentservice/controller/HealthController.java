package org.example.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        log.debug("Health check requested for OrderService");

        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("service", "OrderService");
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", LocalDateTime.now().toString());
        healthStatus.put("version", "1.0.0");

        try {
            HealthComponent health = healthEndpoint.health();
            healthStatus.put("details", health.getStatus() == Status.UP
                    ? "All systems operational"
                    : "Some issues detected");
            healthStatus.put("status", health.getStatus().getCode());
        } catch (Exception e) {
            log.error("Error getting health status: {}", e.getMessage());
            healthStatus.put("details", "Error getting health status");
            healthStatus.put("error", e.getMessage());
        }

        return ResponseEntity.ok(healthStatus);
    }
}