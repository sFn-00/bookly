package com.bookly.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    void init() {
        if (!secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        } else {
            log.warn("Stripe secret key not configured — Stripe integration disabled");
        }
    }
}
