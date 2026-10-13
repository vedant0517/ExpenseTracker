package com.expensetracker.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger documentation configuration with JWT Bearer Authentication scheme.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Provide the Bearer JWT token obtained from /api/auth/login or /api/auth/register"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Expense Tracker REST API")
                        .version("1.0.0")
                        .description("Multi-tenant Personal Expense Tracker backend engineered in Spring Boot 3.3.4 and Java 17. Features JWT authentication, BigDecimal financial analytics, and Flyway database migrations.")
                        .contact(new Contact().name("Vedant Kapgate").email("vedant.kapgate17@gmail.com"))
                );
    }
}
