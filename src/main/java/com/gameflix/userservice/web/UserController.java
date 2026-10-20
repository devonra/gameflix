package com.gameflix.userservice.web;

import com.gameflix.userservice.dto.Dtos.UserResponse;
import com.gameflix.userservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    /** The caller is identified only by the verified token's sub claim, never by a client-supplied id. */
    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        long id;
        try {
            id = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
