package com.millsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Rice Mill Management System API")
                        .version("1.0.0")
                        .description("""
                                Multi-tenant Rice Mill Management System.

                                **Role hierarchy:**
                                - `DEV_ADMIN` — feature flags, mill logos, super admin accounts (no mill data)
                                - `SUPER_ADMIN` — creates mills, manages admin accounts
                                - `MILL_ADMIN` — full access within their mill, manages operators
                                - `MILL_OPERATOR` — day-to-day data entry within their mill

                                **Authentication:** Login via `POST /api/auth/login` to get a JWT token,
                                then click **Authorize** and paste the token (without the `Bearer ` prefix).

                                **Default super admin:** username `superadmin` / password `Admin@123`
                                """)
                        .contact(new Contact().name("Mill System")))
                // No fixed server: springdoc uses the URL the docs were opened on (localhost, Railway, AWS...).
                // Behind a proxy, X-Forwarded-Proto/Host are honoured (server.forward-headers-strategy in prod),
                // so "Try it out" calls https://<your-domain>.
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here (without the 'Bearer' prefix)")));
    }
}
