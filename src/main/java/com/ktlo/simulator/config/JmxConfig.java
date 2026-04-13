package com.ktlo.simulator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * JMX configuration for exposing application metrics via JMX beans.
 *
 * JMX beans will be available at:
 * - com.ktlo.simulator:type=Application,name=KtloSimulator
 * - com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor
 * - com.ktlo.simulator:type=Database,name=HikariCP
 */
@Slf4j
@Configuration
@EnableMBeanExport(defaultDomain = "com.ktlo.simulator", registration = RegistrationPolicy.IGNORE_EXISTING)
public class JmxConfig {

    public JmxConfig() {
        log.info("JMX Configuration initialized - MBeans will be registered under com.ktlo.simulator domain");
    }
}
