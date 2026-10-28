package com.example.demo_jdbc_samples.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Retry Properties Configuration
 *
 * Retry parameters of JdbcRetryConfig (same values as the ScalarDB version).
 * Values are loaded from application.properties with sensible defaults.
 *
 * Default configuration:
 * - Max attempts: 3
 * - Initial delay: 100ms
 * - Max delay: 5000ms (5 seconds)
 * - Multiplier: 2.0 (exponential backoff)
 */
@Getter
@Configuration
public class RetryProperties {

    @Value("${retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${retry.initial-delay-ms:100}")
    private long initialDelayMs;

    @Value("${retry.max-delay-ms:5000}")
    private long maxDelayMs;

    @Value("${retry.multiplier:2.0}")
    private double multiplier;
}
