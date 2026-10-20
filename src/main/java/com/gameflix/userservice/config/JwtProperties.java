package com.gameflix.userservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gameflix.jwt")
public record JwtProperties(@NotBlank String secret,
                            @NotBlank String issuer,
                            @Positive long expirationMinutes) { }
