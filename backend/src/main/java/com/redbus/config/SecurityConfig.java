package com.redbus.config;

import com.redbus.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CorsConfig corsConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Cross-Origin & CSRF Configuration
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Exception Handling with Clean JSON Responses
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )

            // 3. Stateless Session Management (JWT based)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Security Headers
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
            )

            // 5. Authentication Provider
            .authenticationProvider(authenticationProvider())

            // 6. Endpoint Authorization Rules (RBAC)
            .authorizeHttpRequests(auth -> auth
                // Root and Health Endpoints
                .requestMatchers("/", "/health", "/api/health", "/api/v1/health", "/actuator/**", "/favicon.ico", "/error").permitAll()

                // Static file uploads (bus photos, vehicle images)
                .requestMatchers("/uploads/**").permitAll()

                // Public Authentication Endpoints
                .requestMatchers(
                    "/api/auth/login", "/api/v1/auth/login",
                    "/api/auth/register", "/api/v1/auth/register",
                    "/api/auth/operator/register", "/api/v1/auth/operator/register",
                    "/api/auth/verify-email", "/api/v1/auth/verify-email",
                    "/api/auth/resend-verification", "/api/v1/auth/resend-verification",
                    "/api/auth/forgot-password", "/api/v1/auth/forgot-password",
                    "/api/auth/reset-password", "/api/v1/auth/reset-password",
                    "/api/auth/firebase-login", "/api/v1/auth/firebase-login"
                ).permitAll()

                // Authenticated User Profile & Travellers
                .requestMatchers(
                    "/api/auth/me", "/api/v1/auth/me",
                    "/api/auth/profile", "/api/v1/auth/profile",
                    "/api/auth/saved-travellers/**", "/api/v1/auth/saved-travellers/**"
                ).authenticated()

                // Public Catalog Search, Discovery, ML & Coupons (Open for Passengers & Operators)
                .requestMatchers("/api/routes/**", "/api/v1/routes/**").permitAll()
                .requestMatchers("/api/reviews/**", "/api/v1/reviews/**").permitAll()
                .requestMatchers("/api/seats/**", "/api/v1/seats/**").permitAll()
                .requestMatchers("/api/ai/**", "/api/v1/ai/**").permitAll()
                .requestMatchers("/api/seo/**", "/api/v1/seo/**").permitAll()
                .requestMatchers("/api/payments/**", "/api/v1/payments/**").permitAll()
                .requestMatchers("/api/activity/**", "/api/v1/activity/**").permitAll()
                .requestMatchers("/api/ml/**", "/api/v1/ml/**").permitAll()
                .requestMatchers("/api/coupons/**", "/api/v1/coupons/**").permitAll()
                .requestMatchers("/api/operator/coupons/**", "/api/v1/operator/coupons/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()

                // Public Ticket Booking & PNR Lookup
                .requestMatchers(HttpMethod.POST, "/api/bookings", "/api/v1/bookings").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bookings/my-bookings", "/api/v1/bookings/my-bookings").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/bookings/{pnr}", "/api/v1/bookings/{pnr}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bookings/{pnr}/ticket-pdf", "/api/v1/bookings/{pnr}/ticket-pdf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/send-ticket", "/api/v1/bookings/*/send-ticket").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/bookings/*/send-email", "/api/v1/bookings/*/send-email").permitAll()

                // Operator Protected Marketplace Endpoints (ROLE_OPERATOR or ROLE_ADMIN)
                .requestMatchers("/api/operator/**", "/api/v1/operator/**").hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN", "OPERATOR", "ADMIN")

                // Admin Protected Management Endpoints (ROLE_ADMIN)
                .requestMatchers("/api/admin/**", "/api/v1/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                // All Other Endpoints
                .anyRequest().authenticated()
            )

            // 7. JWT Authentication Filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
