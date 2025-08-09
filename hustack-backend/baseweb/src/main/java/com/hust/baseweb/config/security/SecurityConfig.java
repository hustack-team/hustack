package com.hust.baseweb.config.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.education.exception.CustomAccessDeniedHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;


@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    List<String> allowedOrigins;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    String jwkSetUri;

    final SessionRevocationFilter sessionRevocationFilter;

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        final Jwt2AuthenticationConverter jwtConverter = new Jwt2AuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(new Jwt2AuthoritiesConverter());
        jwtConverter.setPrincipalClaimName("preferred_username");

        return jwtConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ServerProperties serverProperties
    ) throws Exception {
        http
            // Enable and configure CORS, CORS should be configured first to avoid browser errors
            .cors(cors -> cors.configurationSource(corsConfigurationSource(allowedOrigins)))

            // Disable HTTP Basic Authentication early if not used
            .httpBasic(AbstractHttpConfigurer::disable)

            // Configure OAuth2 with custom authorities mapping
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .addFilterAfter(sessionRevocationFilter, BearerTokenAuthenticationFilter.class)

            // Add policy enforcement after processing the Bearer token
//            .addFilterAfter(createPolicyEnforcerFilter(), BearerTokenAuthenticationFilter.class)

            // Enable anonymous
//            .anonymous(Customizer.withDefaults())

            // Set session management to STATELESS (mandatory for JWT, state in access-token only)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Disable CSRF as session is not used
            .csrf(AbstractHttpConfigurer::disable)

            // Route security
            .authorizeHttpRequests(authorize -> authorize
                // permission to access all static resources
                .requestMatchers("/resources/**", "/css/**", "/image/**", "/js/**", "/chatSocketHandler/**").permitAll()
                .requestMatchers("/actuator/prometheus/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/videos/videos/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/service/files/**").permitAll()
                .requestMatchers("/edu/assignment/*/submissions").permitAll()
                .requestMatchers("/edu/class/**").hasAnyRole("TEACHER", "STUDENT")
                .requestMatchers("/edu/assignment/**").hasAnyRole("TEACHER", "STUDENT")
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )

            // Do not cache requests as they are handled by the frontend
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))

            // Handle 403 Forbidden errors
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))

            // Configure security headers
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        // If SSL enabled, require HTTPS
        if (serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    @Bean
    public CustomAccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(List<String> allowedOrigins) {
        final CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private ServletPolicyEnforcerFilter createPolicyEnforcerFilter() {
        PolicyEnforcerConfig config;

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
            config = mapper.readValue(
                getClass().getResourceAsStream("/policy-enforcer.json"),
                PolicyEnforcerConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ServletPolicyEnforcerFilter(new ConfigurationResolver() {
            @Override
            public PolicyEnforcerConfig resolve(HttpRequest request) {
                return config;
            }
        });
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).build();
    }

}
