package com.authdemo.auth.service;

import com.authdemo.auth.model.CustomOidcUser;
import com.authdemo.auth.entity.User;
import com.authdemo.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        final OidcUser oidcUser = super.loadUser(userRequest);
        CustomOidcUser newUser = new CustomOidcUser(oidcUser);

        String email = oidcUser.getAttributes().get("email").toString();
        User user = createUserIfNotExists(email, oidcUser);

        newUser.setEmail(email);
        newUser.setFirstName(user.getFirst_name());
        newUser.setLastName(user.getLast_name());

        return newUser;
    }

    public User createUserIfNotExists(String email, OidcUser oidcUser) {
        User user = userRepository.findByEmail(email).orElse(null);
        if(user == null) {
            user = new User();
            user.setEmail(email);
            user.setFirst_name(oidcUser.getGivenName());
            user.setLast_name(oidcUser.getFamilyName());

            userRepository.save(user);
        }

        return user;
    }
}
