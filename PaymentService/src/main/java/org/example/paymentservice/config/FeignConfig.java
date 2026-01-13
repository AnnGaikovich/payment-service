package org.example.paymentservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    @Value("${user.service.api-key:internal-auth-key-12345}")
    private String userServiceApiKey;

    @Value("${order.service.api-key:internal-auth-key-12345}")
    private String orderServiceApiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Передача JWT токена, если он есть
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authorizationHeader = request.getHeader("Authorization");

                if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                    template.header("Authorization", authorizationHeader);
                }
            }

            // Определяем, к какому сервису идет запрос и добавляем соответствующий API ключ
            String url = template.url();
            if (url.contains("users") && userServiceApiKey != null && !userServiceApiKey.isEmpty()) {
                template.header("X-API-Key", userServiceApiKey);
            } else if (url.contains("orders") && orderServiceApiKey != null && !orderServiceApiKey.isEmpty()) {
                template.header("X-API-Key", orderServiceApiKey);
            }
        };
    }
}