package com.authdemo.auth.service;

import com.authdemo.auth.entity.RefreshToken;
import com.authdemo.auth.entity.Role;
import com.authdemo.auth.entity.User;
import com.authdemo.auth.model.AuthTokens;
import com.authdemo.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.between;

@RequiredArgsConstructor
@Service
public class JwtService {
    @Value("${jwt.refresh-token-ttl}")
    private Duration refreshTokenTtl;
    @Value("${jwt.access-token-ttl}")
    private  Duration accessTokenTtl;

    @Value("${spring.application.name}")
    private String issuer;

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    private Jwt generateToken(final String email, final Role role) {
        final var claimSet = JwtClaimsSet.builder()
                .subject(email)
                .issuer(issuer)
                .claim("authorities", role)
                .expiresAt(Instant.now().plus(accessTokenTtl))
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claimSet));
    }

    public AuthTokens generateAuthTokens(final User user) {
        final Jwt accessToken = generateToken(user.getEmail(), user.getRole());

        //TODO: Exchange refresh token to new access token
        final var refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthTokens(accessToken.getTokenValue(), refreshTokenEntity.getId().toString(), between(Instant.now(), Instant.now().plus(accessTokenTtl)));
    }

    public AuthTokens refreshToken(final String refreshToken) {
        final var refreshTokenEntity = refreshTokenRepository.findByIdAndExpiresAtAfter(validateRefreshTokenFormat(refreshToken), Instant.now())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        final var newAccessToken = generateToken(refreshTokenEntity.getUser().getEmail(), refreshTokenEntity.getUser().getRole());

        return new AuthTokens(newAccessToken.getTokenValue(), refreshToken, between(Instant.now(), refreshTokenEntity.getExpiresAt()));
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenRepository.deleteById(validateRefreshTokenFormat(refreshToken));
    }

    private UUID validateRefreshTokenFormat(final String refreshToken) {
        try {
            return UUID.fromString(refreshToken);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
    }
}
