package com.example.demo_jdbc_samples.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JdbcConfig {

    // The dialect follows the database actually connected to
    @Bean
    public SqlDialect sqlDialect(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            String version = connection.getMetaData().getDatabaseProductVersion();
            SqlDialect dialect = SqlDialect.fromProductName(product);
            log.info("Database: {} {} -> dialect {}", product, version, dialect);
            return dialect;
        }
    }
}
