package com.dentallink.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT Authorization";

        // JWT 인증 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        // JWT SecurityScheme 정의
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)"));

        return new OpenAPI()
                .info(new Info()
                        .title("DentalLink 서비스 백엔드 API 명세")
                        .description("병원, 예약, 유저, 결제 등 DentalLink 서비스의 핵심 도메인별 RESTful API 상세 목록을 제공합니다.")
                        .version("1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}