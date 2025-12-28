package org.example.paymentservice.config;

import org.example.paymentservice.auth.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class
JwtConfig {

    @Value("${app.jwt.public-key:classpath:keys/public.key}")
    private Resource publicKeyResource;

    @Bean
    public PublicKey jwtPublicKey() {
        try {
            String publicKeyPem = new String(publicKeyResource.getInputStream().readAllBytes())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWT public key", e);
        }
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(PublicKey jwtPublicKey) {
        return new JwtTokenProvider(jwtPublicKey);
    }
}