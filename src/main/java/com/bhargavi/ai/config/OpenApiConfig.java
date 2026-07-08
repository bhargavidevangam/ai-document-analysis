package com.bhargavi.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentAnalysisOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI Document Analysis API")
                .description("Upload a document and get extracted text with a concise summary.")
                .version("v1")
                .contact(new Contact().name("AI Document Analysis Team")));
    }
}
