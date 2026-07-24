package com.yunong.config;

import com.yunong.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== 公开接口 =====
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // ===== 农户及以上（所有已认证用户）=====
                        .requestMatchers(HttpMethod.GET, "/api/v1/crops/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/weather/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/market/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/farms/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/planting-cycles/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/model-versions/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/diagnosis/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tasks/**").authenticated()
                        // ===== 农户及以上（CUD 操作）=====
                        .requestMatchers(HttpMethod.POST, "/api/v1/farms/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/farms/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/planting-cycles/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/planting-cycles/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tasks/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/diagnosis/upload").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        // ===== 农技人员及以上 =====
                        .requestMatchers("/api/v1/diagnosis/*/review").hasAnyRole("TECHNICIAN", "ADMIN")
                        .requestMatchers("/api/v1/knowledge/**").hasAnyRole("TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/model-versions/**").hasAnyRole("TECHNICIAN", "ADMIN")
                        .requestMatchers("/api/v1/agent-runs/**").hasAnyRole("TECHNICIAN", "ADMIN")
                        .requestMatchers("/api/v1/review-queue/**").hasAnyRole("TECHNICIAN", "ADMIN")
                        // ===== 合作社管理及以上 =====
                        .requestMatchers("/api/v1/monitor/**").hasAnyRole("COOP_MANAGER", "ADMIN")
                        // ===== 仅管理员 =====
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/model-versions/**").hasRole("ADMIN")
                        // ===== 兜底：已认证可访问 =====
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
