package org.example.paymentservice.client;

import org.example.paymentservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "orderServiceClient", url = "${ORDER_SERVICE_URL:http://localhost:8081}", configuration = FeignConfig.class)
public interface OrderServiceClient {

    @GetMapping("/api/orders/{orderId}/exists")
    String existsById(@PathVariable Long orderId);

}