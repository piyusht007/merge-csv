package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI campaignManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Sherwali Agency - File Merging Tool")
                        .description("The API documentation for File merging tool")
                        .contact(new Contact().name("Sherawali Agency").email("support@sherwaliagency.com"))
                        .license(new License().name("COPYRIGHT (c) 2024 Sherawali Agency. All Rights Reserved.").url("http://sherawaliagency.com/")));
    }
}
