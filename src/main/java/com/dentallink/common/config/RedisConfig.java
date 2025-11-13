package com.dentallink.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories
public class RedisConfig {

    // yml 값을 주입합니다.
    @Value("${spring.data.redis.host}") // 호스트
    private String host;
    @Value("${spring.data.redis.port}") // 포트
    private int port;

    // Redis 연결 팩토리 설정
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis 설정
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);

        // SSL/TLS 활성화 (ElastiCache Serverless 필수)
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .build();

        // Lettuce 선택했습니다. (비동기 처리) + SSL 설정 적용
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

    // RedisTemplate 설정 (Set, Get, Delete ... etc)
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {

        // 통신 템플릿
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // key-value 직렬화 방법 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        // hash key-hash value 직렬화 방법 설정
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}