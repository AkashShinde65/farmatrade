package com.farmatrade.bidding.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 *
 * Exposes:
 *  - Swagger UI at /swagger-ui.html
 *  - Raw OpenAPI JSON at /v3/api-docs
 *
 * A "bearerAuth" security scheme is registered globally so every endpoint
 * in Swagger UI shows the "Authorize" button and Postman/curl users know
 * to send: Authorization: Bearer <JWT>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI biddingServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FarmaTrade - Bidding Service (P3)")
                        .description("Live auction, bidding, buy-now, and real-time "
                                + "bid broadcasting API for the FarmaTrade agricultural marketplace.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Anurag").email("anurag@farmatrade.local")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
