package com.hust.baseweb.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloak(KeycloakAdminProperties props) {
        return KeycloakBuilder.builder()
                              .serverUrl(props.getServerUrl())
                              .realm(props.getRealm())
                              .clientId(props.getClientId())
                              .clientSecret(props.getClientSecret())
                              .grantType(props.getGrantType())
                              .build();
    }
}
