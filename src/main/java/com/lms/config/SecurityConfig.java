package com.lms.config;

import com.lms.security.CustomUserDetailsService;
import com.lms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final com.lms.tenant.TenantFilter tenantFilter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // ✅ CRITICAL FIX: allow preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()

                        .requestMatchers(
                                "/auth/**",
                                "/api/auth/**",
                                "/tenants/*/resolve",
                                "/api/tenants/*/resolve",
                                "/tenants/resolve",
                                "/api/tenants/resolve",
                                "/certificates/verify/**",
                                "/api/certificates/verify/**",
                                "/certificates/download/**",
                                "/api/certificates/download/**",
                                "/media/files/**",
                                "/api/media/files/**",
                                "/media/download/**",
                                "/api/media/download/**",
                                "/payments/webhook",
                                "/api/payments/webhook",
                                "/support/contact",
                                "/api/support/contact",
                                "/subscriptions/plans",
                                "/api/subscriptions/plans",
                                "/subscriptions/register-tenant",
                                "/api/subscriptions/register-tenant",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/super-admin/**", "/api/super-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/admin/**", "/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/instructor/**", "/api/instructor/**").hasAnyRole("INSTRUCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/interviews/**", "/api/interviews/**").authenticated()
                        .anyRequest().authenticated()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {


        System.out.println("CORS CONFIG LOADED");

        CorsConfiguration configuration = new CorsConfiguration();

        // Parse env origins
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> allOrigins = new ArrayList<>(origins);

        // ✅ ADD THIS (CRITICAL for your case)
        List<String> prodOrigins = List.of(
                "https://*.vercel.app",
                "https://famiho-lms-services.onrender.com"// ✅ Vercel support (prod + preview)
        );

        // Dev origins
        List<String> devOrigins = List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        );

        prodOrigins.forEach(o -> { if (!allOrigins.contains(o)) allOrigins.add(o); });
        devOrigins.forEach(o -> { if (!allOrigins.contains(o)) allOrigins.add(o); });

        configuration.setAllowedOriginPatterns(allOrigins);

        // ✅ Allow everything cleanly
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setExposedHeaders(List.of("Authorization"));

        // ✅ Required for JWT
        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}