package com.auraspark.note.core.config;

import jakarta.annotation.PostConstruct;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass({SqlSessionFactory.class, DataSource.class})
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private final DataSource dataSource;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void verifyDatabaseConnection() {
        try (var conn = dataSource.getConnection()) {
            log.info("PostgreSQL connection established: {}", conn.getMetaData().getURL());
        } catch (Exception e) {
            log.warn("PostgreSQL is not available at startup: {}", e.getMessage());
        }
    }
}
