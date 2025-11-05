package com.dentallink.common.config;

import com.dentallink.common.utility.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 설정 비활성
        http.csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 요청 권한 설정
        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()//Swagger 접근허용
                        .requestMatchers("/ws/**").permitAll() //챗봇 접근허용
                        .requestMatchers("/api/users/signup", "/api/auth/login/**").permitAll() // 회원가입/로그인만 허용
                        .requestMatchers("/api/hospitals/**").permitAll() //    병원조회는 누구나
                        .requestMatchers("/api/reservations/available-slots").permitAll() //예약 가능시간조회
                        .requestMatchers("/api/admin").permitAll() // 테스트용 어드민 생성 기능입니다.
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(
                                "/payment.html",
                                "/success.html",
                                "/fail.html"
                        ).permitAll()
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 처리
        );

        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 필터 순서 설정: 우리가 만든 JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
