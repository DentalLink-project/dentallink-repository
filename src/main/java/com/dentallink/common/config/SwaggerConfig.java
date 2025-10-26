package com.dentallink.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("DentalLink 서비스 백엔드 API 명세")
                        .description("병원, 예약, 유저, 결제 등 DentalLink 서비스의 핵심 도메인별 RESTful API 상세 목록을 제공합니다.") // 서비스 전체 설명
                        .version("1.0.0"));
    }
}