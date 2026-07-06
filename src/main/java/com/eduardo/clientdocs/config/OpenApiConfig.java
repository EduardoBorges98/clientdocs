package com.eduardo.clientdocs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clientDocsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClientDocs Processor API")
                        .description("Backend API for client registration and mock document processing.")
                        .version("v1.0.0"));
    }
}