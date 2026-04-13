package com.ktlo.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for KTLO Simulator.
 * This application simulates various KTLO (Keep The Lights On) scenarios including:
 * - Threadpool exhaustion with high CPU load
 * - Database connection failures and timeouts
 * - Comprehensive logging and monitoring via JMX
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class KtloSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(KtloSimulatorApplication.class, args);
    }
}
