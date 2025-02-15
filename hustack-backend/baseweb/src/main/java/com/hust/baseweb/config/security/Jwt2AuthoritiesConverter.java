package com.hust.baseweb.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a converter for roles as embedded in the JWT by a Keycloak server
 * Roles are taken from both realm_access.roles & resource_access.{client}.roles
 */
public class Jwt2AuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Value("${keycloak.allowed-client-ids:}")
    private Set<String> allowedClientIds;

    /**
     * @param jwt
     * @return
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        this.allowedClientIds = allowedClientIds != null ? new HashSet<>(allowedClientIds) : Collections.emptySet();

        final Map<String, Object> claims = jwt.getClaims();

        // Get roles from realm_access
        final Map<String, Object> realmAccess = (Map<String, Object>) claims.getOrDefault(
            "realm_access",
            Collections.emptyMap());
        final Collection<String> realmRoles = (Collection<String>) realmAccess.getOrDefault(
            "roles",
            Collections.emptyList());

        // Get roles from clients in resource_access
        final Map<String, Object> resourceAccess = (Map<String, Object>) claims.getOrDefault(
            "resource_access",
            Collections.emptyMap());
        final Collection<String> clientRoles = resourceAccess.entrySet().stream()
//                                                             .filter(entry -> entry.getValue() instanceof Map)
                                                             .filter(entry -> allowedClientIds.isEmpty() ||
                                                                              allowedClientIds.contains(entry.getKey()))
                                                             .map(entry -> (Map<String, Object>) entry.getValue())
                                                             .map(client -> (Collection<String>) client.getOrDefault(
                                                                 "roles",
                                                                 Collections.emptyList()))
                                                             .flatMap(Collection::stream)
                                                             .toList();

        return Stream.concat(realmRoles.stream(), clientRoles.stream())
                     .map(role -> "ROLE_" + role.toUpperCase())
                     .map(SimpleGrantedAuthority::new)
                     .collect(Collectors.toList());
    }
}
