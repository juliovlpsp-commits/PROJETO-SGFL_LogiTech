package com.logitech.sgfl.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            UserDetailsService userDetailsService
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/icons.svg"
                        ).permitAll()

                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/rastreio/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auth/session")
                        .authenticated()

                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        /*
                         * Exclusão continua sendo ADMIN.
                         */
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/**"
                        ).hasRole("ADMIN")

                        /*
                         * Frota: ADMIN.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/veiculos/**",
                                "/api/motoristas/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/veiculos/**",
                                "/api/motoristas/**"
                        ).hasRole("ADMIN")

                        /*
                         * Produtos e estoque: ADMIN.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/produtos/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/produtos/**"
                        ).hasRole("ADMIN")

                        /*
                         * Cadastro/alteração de clientes:
                         * ADMIN ou OPERADOR.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/clientes/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "OPERADOR"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/clientes/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "OPERADOR"
                        )

                        /*
                         * Auditoria transversal: ADMIN.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auditoria/**"
                        ).hasRole("ADMIN")

                        /*
                         * Listagens, pedidos e fluxo de entregas:
                         * usuário autenticado.
                         */
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authenticationProvider(
                        authenticationProvider()
                )
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite("Lax"));
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                allowedOrigins
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-XSRF-TOKEN",
                        "X-Request-ID"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(
                passwordEncoder()
        );

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}
