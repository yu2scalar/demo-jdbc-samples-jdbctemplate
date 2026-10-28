package com.example.demo_jdbc_samples.config;

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JdbcConfig {

    // Logs the database actually connected to. The SQL is the same for every database
    // (no upsert statement, DELETE instead of TRUNCATE), so nothing depends on it.
    @Bean
    public ApplicationRunner logDatabase(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                log.info("Database: {} {} ({})", connection.getMetaData().getDatabaseProductName(),
                    connection.getMetaData().getDatabaseProductVersion(), connection.getMetaData().getURL());
            }
        };
    }
}
