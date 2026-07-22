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
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Farmer and above
                        .requestMatchers(HttpMethod.GET, "/api/v1/crops/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/weather/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/market/**").hasAnyRole("FARMER", "TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        // Technician and above
                        .requestMatchers("/api/v1/diagnosis/*/review").hasAnyRole("TECHNICIAN", "ADMIN")
                        .requestMatchers("/api/v1/knowledge/**").hasAnyRole("TECHNICIAN", "COOP_MANAGER", "ADMIN")
                        // Coop manager and above
                        .requestMatchers("/api/v1/monitor/**").hasAnyRole("COOP_MANAGER", "ADMIN")
                        // Admin only
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        // Authenticated for everything else
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
