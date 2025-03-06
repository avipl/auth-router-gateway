package com.authdemo.auth.service;

import com.authdemo.auth.entity.RefreshToken;
import com.authdemo.auth.model.AuthTokens;
import com.authdemo.auth.dto.SignInRequestDto;
import com.authdemo.auth.entity.User;
import com.authdemo.auth.repository.RefreshTokenRepository;
import com.authdemo.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.between;

@Service
@RequiredArgsConstructor
public class SignInService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthTokens authenticate(final SignInRequestDto requestDto) {
        final var authToken = UsernamePasswordAuthenticationToken.unauthenticated(requestDto.email(), requestDto.password());
        final var authentication = authenticationManager.authenticate(authToken);

        final var user = userRepository.findByEmail(requestDto.email()).orElseThrow(() ->
                new UsernameNotFoundException("User with email %s not found".formatted(requestDto.email())));

        return jwtService.generateAuthTokens(user);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = null;
        if(authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            String email = (String) oidcUser.getAttributes().get("email");
            user = userRepository.findByEmail(email).orElse(null);
        } else if(authentication.getPrincipal() instanceof Jwt) {
            Jwt authenticationToken = (Jwt) authentication.getPrincipal();
            String email = authenticationToken.getSubject();

            user = userRepository.findByEmail(email).orElse(null);
        }

        return user;
    }
}
