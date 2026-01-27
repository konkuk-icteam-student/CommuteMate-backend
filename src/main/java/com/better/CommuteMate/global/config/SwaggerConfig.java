package com.better.CommuteMate.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("JWT", jwtAuth())
                )
                .info(apiInfo());
    }

    private SecurityScheme jwtAuth() {
        return new SecurityScheme()
                .type(Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(In.HEADER)
                .name(HttpHeaders.AUTHORIZATION);
    }

    private Info apiInfo() {
        return new Info()
                .title("CommuteMate API Docs")
                .description("CommuteMate Swagger API 문서")
                .version("v1");
    }
}
