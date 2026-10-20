package com.gameflix.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gameflix.userservice.model.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public final class Dtos {
    private Dtos() { }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(min = 3, max = 50) String username) {

        @AssertTrue(message = "password is too long once encoded (bcrypt limit is 72 bytes)")
        public boolean isPasswordWithinByteLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) { }

    // Deliberately has no password field: the hash can never be serialized
    public record UserResponse(Long id, String email, String username, String role, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getRole(), u.getCreatedAt());
        }
    }

    public record TokenResponse(String token, String tokenType, long expiresInSeconds) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(String error, String message, Map<String, String> details) { }


}
