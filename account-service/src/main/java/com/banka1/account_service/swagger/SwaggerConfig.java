package com.banka1.account_service.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * Naziv aplikacije koji se prikazuje u Swagger UI.
     */
    private static final String APP_TITLE = "Account service API";

    /**
     * Opis API-ja koji se prikazuje u Swagger UI.
     */
    private static final String APP_DESCRIPTION = "API for accounts";

    /**
     * Verzija API-ja.
     */
    private static final String APP_VERSION = "1.0";

    /**
     * Konfigurise OpenAPI specifikaciju za servis i bearer autentikaciju.
     * Dodaje JWT Bearer security shemu i osnovne informacije o API-ju.
     *
     * @return OpenAPI opis servisa
     */
    @Configuration
    public class OpenApiConfig {

        @Bean
        public OpenAPI openAPI() {
            return new OpenAPI()
                    .info(new Info()
                            .title("Client Service API")
                            .description("Servis za upravljanje klijentima banke. Dostupan samo zaposlenima.")
                            .version("1.0.0"))
                    .addSecurityItem(new SecurityRequirement().addList("BearerAuthentication"))
                    .components(new Components()
                            .addSecuritySchemes("BearerAuthentication",
                                    new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")));
        }
    }
}