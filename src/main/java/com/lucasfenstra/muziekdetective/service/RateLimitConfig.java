package com.lucasfenstra.muziekdetective.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter analyzeRateLimiter(
            @Value("${muziekdetective.ratelimit.max-requests:5}") int maxRequests,
            @Value("${muziekdetective.ratelimit.window:10m}") Duration window) {
        return new RateLimiter(maxRequests, window);
    }
}
