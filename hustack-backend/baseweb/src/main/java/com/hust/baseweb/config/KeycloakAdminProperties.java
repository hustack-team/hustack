package com.hust.baseweb.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    String serverUrl;

    String realm;

    String clientId;

    String clientSecret;

    String grantType;

    long defaultAccessTokenLifespan;

    long defaultBonusTtl;

}
