package com.authdemo.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.method;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class GatewayRouterConfig {
    @Bean
    public RouterFunction<ServerResponse> routeConfig() {
        // TODO: add service routes
        return route()
                .before(stripPrefix(1))
                .route(RequestPredicates.path("/example/**").and(method(POST, GET, PUT)), http("http://localhost:3000"))
                .onError(Exception.class, this::handleException)
                .build();
    }

    private ServerResponse handleException(Throwable throwable, ServerRequest request) {
        throwable.printStackTrace();
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
