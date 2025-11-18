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
        http.csrf(AbstractHttpConfigurer::disable);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll()

                .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                ).permitAll()

                .requestMatchers("/api/users/signup").permitAll()
                .requestMatchers("/api/auth/login/**").permitAll()

                .requestMatchers("/api/hospitals/**").permitAll()
                .requestMatchers("/api/reservations/available-slots").permitAll()
                .requestMatchers("/api/admin").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                .requestMatchers(
                        "/payment.html",
                        "/success.html",
                        "/fail.html",
                        "/",
                        "/index.html",
                        "/hospitals/**",
                        "/hospital.html",
                        "/reservation.html",
                        "/reservation",
                        "/chatbot",
                        "/chatbot.html",
                        "/admin-chat.html",
                        "/my-reservations",
                        "/my-reservations.html",
                        "/my-page",
                        "/my-page.html"
                ).permitAll()

                .requestMatchers("/*.css", "/*.js", "/*.ico").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                .anyRequest().authenticated()
        );

        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:8000",
                "http://127.0.0.1:8000",
                "http://localhost:63342",
                "http://127.0.0.1:63342",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://13.124.156.240:8080",
                "https://www.dentallink.store",
                "https://dentallink.store"
        ));

        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Accept",
                "Origin",
                "Refresh-Token"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowCredentials(true);

        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Refresh-Token",
                "Content-Disposition"
        ));

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 기존 공개 API
        source.registerCorsConfiguration("/api/hospitals/**", configuration);
        source.registerCorsConfiguration("/api/auth/**", configuration);
        source.registerCorsConfiguration("/api/reservations/**", configuration);
        source.registerCorsConfiguration("/api/point-log/**", configuration); // 오타 수정됨
        source.registerCorsConfiguration("/api/reviews/**", configuration);
        source.registerCorsConfiguration("/api/users/**", configuration);
        source.registerCorsConfiguration("/api/payments/**", configuration);

        // WebSocket
        source.registerCorsConfiguration("/ws/**", configuration);

        return source;
    }
}
