package com.gameflix.userservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final int BCRYPT_STRENGTH = 12;   // OWASP minimum is 10; see README

    /** Delegating encoder: stored hashes look like {bcrypt}$2a$12$..., so algorithms can be migrated later. */
    @Bean
    PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder(BCRYPT_STRENGTH));
        return new DelegatingPasswordEncoder("bcrypt", encoders);
    }

    /** HS256 key from configuration. Fails at startup if the secret is shorter than 256 bits. */
    @Bean
    SecretKey jwtSigningKey(JwtProperties props) {
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("gameflix.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /** Algorithm is pinned to HS256 (the token header is never trusted); exp/nbf and iss are validated. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, ex) -> {
            response.setStatus(401);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing, invalid, or expired token\"}");
        };

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())   // stateless API, token in Authorization header, no cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint(unauthorized)
                .jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${gameflix.cors.allowed-origin}") String origin) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(origin));   // one named origin, never "*"
        cfg.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
