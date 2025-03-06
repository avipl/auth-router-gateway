package com.authdemo.auth.config;

import com.authdemo.auth.component.AuthTokenResolver;
import com.authdemo.auth.filter.RateLimitFilter;
import com.authdemo.auth.service.CustomOidcUserService;
import com.authdemo.auth.entity.Role;
import com.authdemo.auth.handler.OAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.ConcurrentSessionFilter;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;

    private final AuthTokenResolver authTokenResolver;

    private final CustomOidcUserService customOidcUserService;

    private final OAuthSuccessHandler successHandler;

    CookieCsrfTokenRepository tokenRepository = new CookieCsrfTokenRepository();

    XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();
    CsrfTokenRequestHandler requestHandler = delegate::handle;

    @Bean
    public SecurityFilterChain securityWebFilterChain(final HttpSecurity http) throws Exception {
        tokenRepository.setCookieCustomizer(c ->
                c.secure(true)
                        .httpOnly(true)
                        .build()
        );

        http
            .csrf((csrf) ->
                csrf.csrfTokenRepository(tokenRepository)
                        .csrfTokenRequestHandler(requestHandler)
            )
            .addFilterAfter(rateLimitFilter, ConcurrentSessionFilter.class)
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository())
                    .requireExplicitSave(true)
            )
            .authorizeHttpRequests((exchange) -> {
                exchange
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/error")
                        .permitAll();

                exchange.requestMatchers("/auth/v1/login",
                        "/auth/v1/signup")
                        .anonymous();

                /**
                 * Phone number verification api
                 */
                exchange.requestMatchers("/auth/v1/verify/*")
                        .hasAnyAuthority(Role.GUEST.toString(), Role.ADMIN.toString(), Role.SUPER.toString());

                /**
                 *  allow access to other apis if phone number is verified or admin level user
                 */
                exchange.anyRequest()
                        .hasAnyAuthority(Role.VERIFIED.toString(), Role.SUPER.toString(), Role.ADMIN.toString());
            }).sessionManagement(session ->
                session
                        .sessionCreationPolicy(SessionCreationPolicy.NEVER) // Don't create session for anonymous user
                        .maximumSessions(1)
            )
            .oauth2ResourceServer(server -> server
                        .bearerTokenResolver(authTokenResolver)
                        .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            ).oauth2Login(oauth2 ->
                        oauth2
                                .userInfoEndpoint(uie ->
                                        uie
                                                .oidcUserService(customOidcUserService)
                                ).successHandler(successHandler)
                )
                .exceptionHandling(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            final AuthenticationConfiguration authenticationConfiguration
            ) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
