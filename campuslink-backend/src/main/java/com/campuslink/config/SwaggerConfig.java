package com.campuslink.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration de la documentation OpenAPI / Swagger.
 *
 * <p>Expose la documentation interactive de l'API sur {@code /swagger-ui.html}
 * et déclare le schéma de sécurité JWT (Bearer Token) utilisé par
 * l'ensemble des endpoints protégés.</p>
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI campusLinkOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Environnement local")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("CampusLink API")
                .description("Documentation officielle de l'API backend de la plateforme CampusLink")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Équipe CampusLink")
                        .email("support@campuslink.io"))
                .license(new License()
                        .name("Propriétaire")
                        .url("https://campuslink.io"));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Insérez le token JWT au format : Bearer {token}");
    }

}
