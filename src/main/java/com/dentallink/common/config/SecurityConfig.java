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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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

        // CORS 설정 활성화
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 요청 권한 설정
        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests

                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/*.css", "/*.js", "/*.ico").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 명시적 Origin 지정 (로컬 개발 포트 + 외부 서버)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:8000",
            "http://127.0.0.1:8000",
            "http://localhost:63342",
            "http://127.0.0.1:63342",
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://43.203.215.197:8080"
        ));

        // 2. 필요한 헤더만 명시
        configuration.setAllowedHeaders(List.of(
            "Content-Type",
            "Authorization",
            "Accept",
            "Origin",
            "Refresh-Token"
        ));

        // 3. 필요한 HTTP 메서드만 명시
        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
        ));

        // 4. 인증정보 포함 허용
        configuration.setAllowCredentials(true);

        // 5. 클라이언트에 노출할 헤더
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Refresh-Token",
            "Content-Disposition"
        ));

        // 6. preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 7. 공개 API 경로만 CORS 허용 (관리자/민감한 엔드포인트 제외)
        source.registerCorsConfiguration("/api/hospitals/**", configuration);
        source.registerCorsConfiguration("/api/auth/**", configuration);
        source.registerCorsConfiguration("/api/reservations/**", configuration);
        source.registerCorsConfiguration("/api/point-logs/**", configuration);
        source.registerCorsConfiguration("/api/reviews/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);

        return source;
    }
}
