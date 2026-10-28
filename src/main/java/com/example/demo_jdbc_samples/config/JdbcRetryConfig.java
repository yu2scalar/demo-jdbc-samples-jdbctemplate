package com.example.demo_jdbc_samples.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.TransientDataAccessException;

/**
 * Retry of a whole transaction, with the same parameters as the ScalarDB version
 * (RetryProperties).
 *
 * Only failures that the database reports as worth retrying are retried:
 * TransientDataAccessException and its subtypes, e.g. a deadlock (PostgreSQL 40P01,
 * Oracle ORA-00060), a serialization failure (40001, ORA-08177) or a lock timeout.
 * Everything else is not retried: business rejections and not-found
 * (CustomException), a duplicate key, other SQL errors, and a failed commit
 * (TransactionRunner turns it into 9200 because the outcome is unknown).
 */
@Configuration
public class JdbcRetryConfig {

    @Autowired
    private RetryProperties retryProperties;

    @Bean(name = "jdbcRetryTemplate")
    public RetryTemplate jdbcRetryTemplate() {
        // retry.max-attempts is the TOTAL attempt count; maxRetries counts the re-attempts
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .includes(TransientDataAccessException.class)
                .maxRetries(retryProperties.getMaxAttempts() - 1)
                .delay(Duration.ofMillis(retryProperties.getInitialDelayMs()))
                .multiplier(retryProperties.getMultiplier())
                .maxDelay(Duration.ofMillis(retryProperties.getMaxDelayMs()))
                .build();
        return new RetryTemplate(retryPolicy);
    }
}
