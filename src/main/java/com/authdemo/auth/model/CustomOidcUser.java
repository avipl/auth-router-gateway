package com.authdemo.auth.model;

import com.authdemo.auth.entity.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class CustomOidcUser implements OidcUser, Serializable {

    private final OidcUser oidcUser;
    private String email;
    private String firstName;
    private String lastName;

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorityList = new ArrayList<>();
        oidcUser.getAuthorities().forEach(ga -> authorityList.add(ga));
        authorityList.add(new SimpleGrantedAuthority(String.valueOf(Role.GUEST)));

        return authorityList;
    }

    @Override
    public String getName() {
        return oidcUser.getName();
    }
}
