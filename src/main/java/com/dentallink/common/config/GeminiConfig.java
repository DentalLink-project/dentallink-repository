package com.dentallink.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiConfig {

    private String apiKey;

    /**
     * 사용할 모델 이름
     * 기본값: gemini-2.0-flash (무료 티어)
     */
    private String modelName = "gemini-2.0-flash";

    /**
     * 최대 토큰 수
     */
    private Integer maxTokens = 2048;

    /**
     * Temperature (창의성 조절: 0.0 ~ 1.0)
     * 0에 가까울수록 일관된 응답, 1에 가까울수록 창의적인 응답
     */
    private Double temperature = 0.7;

    /**
     * Top-P (다양성 조절: 0.0 ~ 1.0)
     */
    private Double topP = 0.95;

    /**
     * Top-K (어휘 선택 범위)
     */
    private Integer topK = 40;

    /**
     * API 요청 타임아웃 (초)
     */
    private Integer timeout = 30;

    /**
     * Rate Limit 설정
     */
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {
        /**
         * 분당 최대 요청 수 (Gemini 무료: 15)
         */
        private Integer perMinute = 15;

        /**
         * 사용자당 분당 최대 요청 수
         */
        private Integer perUserPerMinute = 10;

        /**
         * 일일 최대 요청 수 (Gemini 무료: 1500)
         */
        private Integer perDay = 1500;
    }
}
