package com.kalamet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI kalametOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kalamet API")
                        .version("v1")
                        .description("""
                                REST API of the Kalamet store. Money is in whole Rials (show Toman = Rial / 10);
                                timestamps are UTC ISO-8601. Sign in with POST /api/auth/otp then
                                POST /api/auth/verify, and send the access token as a Bearer token."""))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
