package com.millsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
                                - `SUPER_ADMIN` — creates mills, manages admin accounts
                                - `MILL_ADMIN` — full access within their mill, manages operators
                                - `MILL_OPERATOR` — day-to-day data entry within their mill

                                **Authentication:** Login via `POST /api/auth/login` to get a JWT token,
                                then click **Authorize** and enter `Bearer <token>`.

                                **Default super admin:** username `superadmin` / password `Admin@123`
                                """)
                        .contact(new Contact().name("Mill System")))
                .servers(List.of(new Server().url("http://localhost:3001").description("Local")))
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
