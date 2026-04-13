package com.ktlo.simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for API documentation.
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI ktloSimulatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KTLO Simulator API")
                        .description("Java simulation application for demonstrating KTLO (Keep The Lights On) activities and failure scenarios. " +
                                "This API provides endpoints to simulate CPU exhaustion, database failures, and monitor application health.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KTLO Simulator Team")
                                .email("manish.singh07@nagarro.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8090")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://your-azure-vm:8090")
                                .description("Azure VM Production Server")
                ));
    }
}
