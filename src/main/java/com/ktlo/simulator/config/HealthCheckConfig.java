package com.ktlo.simulator.config;

import org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Custom health check configuration.
 * Only checks the primary datasource, excludes simulation datasources.
 */
@Configuration
@AutoConfigureBefore(DataSourceHealthContributorAutoConfiguration.class)
public class HealthCheckConfig {

    /**
     * Create a custom health indicator that only checks the primary datasource.
     * This prevents health checks from failing due to intentionally broken simulation datasources.
     */
    @Primary
    @Bean(name = "dbHealthIndicator")
    public HealthIndicator dbHealthIndicator(DataSource primaryDataSource) {
        return new DataSourceHealthIndicator(primaryDataSource, "SELECT 1");
    }
}
