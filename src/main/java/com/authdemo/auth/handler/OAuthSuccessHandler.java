package com.authdemo.auth.handler;

import com.authdemo.auth.entity.RefreshToken;
import com.authdemo.auth.entity.User;
import com.authdemo.auth.model.AuthTokens;
import com.authdemo.auth.repository.RefreshTokenRepository;
import com.authdemo.auth.repository.UserRepository;
import com.authdemo.auth.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.authdemo.auth.util.CookieUtil.addCookie;
import static java.time.Duration.between;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //TODO: Redirect to referer i.e. frontend

        User user = getUser(authentication);
        AuthTokens authTokens = jwtService.generateAuthTokens(user);

        response.setHeader(SET_COOKIE, addCookie(AuthTokens.ACCESS_TOKEN_COOKIE_NAME, authTokens.accessToken(), authTokens.accessTokenTtl()).toString());

        this.delegate.onAuthenticationSuccess(request, response, authentication);
    }

    private User getUser(Authentication authentication) {
        User user = null;
        if(authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            String email = (String) oidcUser.getAttributes().get("email");
            user = userRepository.findByEmail(email).orElse(null);
            log.info("OidcUser");
        } else if(authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = (String) oAuth2User.getAttributes().get("email");
            user = userRepository.findByEmail(email).orElse(null);
            log.info("OAuth2User");
        }

        return user;
    }
}
