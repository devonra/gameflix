package com.gameflix.userservice.service;

import com.gameflix.userservice.config.JwtProperties;
import com.gameflix.userservice.dto.Dtos.TokenResponse;
import com.gameflix.userservice.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    /** Payload holds only non-confidential claims: sub (user id), iss, iat, exp, role. */
    public TokenResponse issue(User user) {
        Instant now = Instant.now();
        Duration lifetime = Duration.ofMinutes(props.expirationMinutes());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .claim("role", user.getRole())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", lifetime.toSeconds());
    }
}
