package com.aipanelist.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI aiPanelistOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Panelist System API")
                        .description("Multi-agent AI analysis system for research documents using NVIDIA models")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Panelist Team")
                                .email("support@aipanelist.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://aipanelist.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:80")
                                .description("Production Server (Nginx)")
                ));
    }
}
