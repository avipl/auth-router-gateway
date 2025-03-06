package com.authdemo.auth.model;

import java.time.Duration;

public record AuthTokens(String accessToken, String refreshToken, Duration accessTokenTtl) {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "ACCESS_TOKEN";

}
