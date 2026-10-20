package com.gameflix.userservice.service;

import com.gameflix.userservice.dto.Dtos.LoginRequest;
import com.gameflix.userservice.dto.Dtos.RegisterRequest;
import com.gameflix.userservice.dto.Dtos.TokenResponse;
import com.gameflix.userservice.model.User;
import com.gameflix.userservice.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    public static class EmailAlreadyRegisteredException extends RuntimeException { }
    public static class InvalidCredentialsException extends RuntimeException { }

    private static final int MAX_PASSWORD_LENGTH = 64;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    /** Uniqueness is enforced by the DB constraint; the 409 is driven by that violation, not a pre-check. */
    public User register(RegisterRequest req) {
        User user = new User(normalize(req.email()), req.username().trim(),
                encoder.encode(req.password()), "USER");
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    /** Every credential failure throws the same exception, so the client sees one generic message. */
    public TokenResponse login(LoginRequest req) {
        if (req.password().length() > MAX_PASSWORD_LENGTH) {
            throw new InvalidCredentialsException();
        }
        User user = users.findByEmail(normalize(req.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return jwtService.issue(user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
