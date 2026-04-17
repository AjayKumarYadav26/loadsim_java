package com.ktlo.simulator.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Database configuration with multiple DataSource beans for failure simulation.
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    /**
     * Primary DataSource - normal Azure PostgreSQL connection.
     * This is used by Spring Data JPA by default.
     */
    @Primary
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource() {
        log.info("Creating primary DataSource");
        return createDataSource(datasourceUrl, datasourceUsername, datasourcePassword, 30000);
    }

    /**
     * Timeout DataSource - very low connection timeout for timeout simulation.
     * Using 500ms for safe operation while still simulating timeouts.
     */
    @Lazy
    @Bean(name = "timeoutDataSource")
    public DataSource timeoutDataSource() {
        log.info("Creating timeout DataSource (500ms timeout)");
        return createDataSource(datasourceUrl, datasourceUsername, datasourcePassword, 500, false);
    }

    /**
     * Invalid DataSource - wrong JDBC URL for connection failure simulation.
     * This bean is lazy and will only initialize when actually used.
     */
    @Lazy
    @Bean(name = "invalidDataSource")
    public DataSource invalidDataSource() {
        log.info("Creating invalid DataSource");
        String invalidUrl = "jdbc:postgresql://invalid-host.postgres.database.azure.com:5432/code_test?sslmode=require&connectTimeout=5";
        return createDataSource(invalidUrl, datasourceUsername, datasourcePassword, 5000, false);
    }

    /**
     * Auth Failure DataSource - wrong credentials.
     */
    @Lazy
    @Bean(name = "authFailureDataSource")
    public DataSource authFailureDataSource() {
        log.info("Creating auth failure DataSource");
        return createDataSource(datasourceUrl, datasourceUsername, "wrong_password", 5000, false);
    }

    /**
     * Helper method to create HikariDataSource with custom settings.
     */
    private DataSource createDataSource(String url, String username, String password, long connectionTimeout) {
        return createDataSource(url, username, password, connectionTimeout, true);
    }

    /**
     * Helper method to create HikariDataSource with custom settings.
     *
     * @param validateOnStartup Set to false to disable connection validation on startup
     */
    private DataSource createDataSource(String url, String username, String password, long connectionTimeout, boolean validateOnStartup) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        config.setConnectionTimeout(connectionTimeout);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(2000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("KtloHikariPool-" + connectionTimeout + "ms");

        // Disable startup validation for failure simulation DataSources
        if (!validateOnStartup) {
            config.setInitializationFailTimeout(-1); // Don't fail on startup
        }

        // Disable JMX registration - use Spring Actuator metrics instead
        config.setRegisterMbeans(false);

        return new HikariDataSource(config);
    }
}
