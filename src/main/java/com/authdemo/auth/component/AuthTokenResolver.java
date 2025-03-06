package com.authdemo.auth.component;

import com.authdemo.auth.model.AuthTokens;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import static org.springframework.web.util.WebUtils.getCookie;

@Component
public class AuthTokenResolver implements BearerTokenResolver {

    @Override
    public String resolve(final HttpServletRequest request) {
        Cookie cookie = getCookie(request, AuthTokens.ACCESS_TOKEN_COOKIE_NAME);
        if(cookie != null) return cookie.getValue();
        else return new DefaultBearerTokenResolver().resolve(request);
    }
}
