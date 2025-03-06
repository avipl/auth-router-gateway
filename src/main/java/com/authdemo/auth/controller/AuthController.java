package com.authdemo.auth.controller;

import com.authdemo.auth.model.AuthTokens;
import com.authdemo.auth.repository.UserRepository;
import com.authdemo.auth.service.SignInService;
import com.authdemo.auth.dto.RegistrationRequestDto;
import com.authdemo.auth.dto.SignInRequestDto;
import com.authdemo.auth.dto.SignInResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.authdemo.auth.entity.User;

import java.time.Duration;

import static com.authdemo.auth.util.CookieUtil.addCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SignInService signInService;

    @Value("${cs-token-ttl}")
    private Duration tokenTtl;

    @PostMapping(value = "/v1/signup", consumes = "application/json")
    public ResponseEntity<String> signup(@Valid @RequestBody final RegistrationRequestDto requestDto) {
        try {
            if (userRepository.existsByEmail(requestDto.email()))
                return ResponseEntity.status(CONFLICT).body("Email [%s] is already taken".formatted(requestDto.email()));

            registerUser(requestDto);
            final AuthTokens authTokens = signInService.authenticate(new SignInRequestDto(requestDto.email(), requestDto.password()));

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(SET_COOKIE, addCookie(AuthTokens.ACCESS_TOKEN_COOKIE_NAME, authTokens.accessToken(), authTokens.accessTokenTtl()).toString())
                    .body("Registration successful");
        } catch (Exception e) {
            log.error(e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed");
        }
    }

    @PostMapping(value = "/v1/login", consumes = "application/json")
    public ResponseEntity signIn(@RequestBody final SignInRequestDto requestDto) {
        try {
            final var authTokens = signInService.authenticate(requestDto);

            return ResponseEntity
                    .status(OK)
                    .header(SET_COOKIE, addCookie(AuthTokens.ACCESS_TOKEN_COOKIE_NAME, authTokens.accessToken(), authTokens.accessTokenTtl()).toString())
                    .body(new SignInResponseDto(authTokens.accessToken()));
        } catch (Exception e) {
            log.error(e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login Failed");
        }
    }

    private void registerUser(RegistrationRequestDto requestDto) {
        User user = new User();
        user.setEmail(requestDto.email());
        user.setPass(passwordEncoder.encode(requestDto.password()));

        userRepository.save(user);
    }
}