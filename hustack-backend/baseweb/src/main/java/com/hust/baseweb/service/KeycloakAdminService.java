package com.hust.baseweb.service;

import com.hust.baseweb.config.KeycloakAdminProperties;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakAdminService {

    final Keycloak keycloak;

    final KeycloakAdminProperties keycloakAdminProperties;

    volatile String studentGroupId;

    public String getStudentGroupId() {
        if (studentGroupId == null) {
            synchronized (this) {
                if (studentGroupId == null) {
                    studentGroupId = fetchStudentGroupId();
                }
            }
        }
        return studentGroupId;
    }

    private String fetchStudentGroupId() {
        String realm = keycloakAdminProperties.getRealm();
        List<GroupRepresentation> groups = keycloak.realm(realm).groups().groups();
        return groups.stream()
                     .filter(g -> g.getName().equalsIgnoreCase("STUDENT"))
                     .map(GroupRepresentation::getId)
                     .findFirst()
                     .orElseThrow(() -> new RuntimeException("Group STUDENT not found"));
    }

    public boolean createUser(UserRepresentation user, String password) {
        boolean result;
        String realm = keycloakAdminProperties.getRealm();

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

                // Set password
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
//                credential.setTemporary(true); // Require password change on first login
                credential.setValue(password);
                keycloak.realm(realm).users().get(userId).resetPassword(credential);

                // Assign groups
                String studentGroupId = getStudentGroupId();
                keycloak.realm(realm).users().get(userId).joinGroup(studentGroupId);

                result = true;
            } else {
                result = false;
            }
        }

        return result;
    }

    public void updateEnabledUser(String username, boolean enabled) {
        if (!StringUtils.isBlank(username)) {
            String realm = keycloakAdminProperties.getRealm();
            List<UserRepresentation> users = keycloak.realm(realm)
                                                     .users()
                                                     .searchByUsername(username, true);

            for (UserRepresentation user : users) {
                if (username.equalsIgnoreCase(user.getUsername())) {
                    user.setEnabled(enabled);
                    keycloak.realm(realm)
                            .users()
                            .get(user.getId())
                            .update(user);
                }
            }
        }
    }

    public void resetPassword(String username, String newPassword, boolean throwIfNotFound) {
        String realm = keycloakAdminProperties.getRealm();
        List<UserRepresentation> users = keycloak.realm(realm)
                                                 .users()
                                                 .searchByUsername(username, true);
        if (users.isEmpty()) {
            if (throwIfNotFound) {
                throw new IllegalArgumentException("User with username: " + username + " not found in realm: " + realm);
            } else {
                log.info("User with username: {} not found in realm: {}", username, realm);
                return;
            }
        }

        if (users.size() > 1) {
            log.warn("Multiple users found with username: {} in realm: {}. Using the first match", username, realm);
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);  // does not require password change on login
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);

        String userId = users.get(0).getId();
        keycloak.realm(realm)
                .users()
                .get(userId)
                .resetPassword(credential);
    }

    public void logout(String username) {
        String realm = keycloakAdminProperties.getRealm();
        List<UserRepresentation> users = keycloak.realm(realm)
                                                 .users()
                                                 .searchByUsername(username, true);

        if (users.isEmpty()) {
            log.info("User with username: {} not found in realm: {}", username, realm);
            return;
        }

        if (users.size() > 1) {
            log.warn("Multiple users found with username: {} in realm: {}. Using the first match", username, realm);
        }

        String userId = users.get(0).getId();
        keycloak.realm(realm).users().get(userId).logout();
    }

    public void deleteUserIfExists(String username) {
        if (StringUtils.isBlank(username)) {
            log.info("Username is blank, skip deleting user from Keycloak");
            return;
        }

        String realm = keycloakAdminProperties.getRealm();
        List<UserRepresentation> users = keycloak.realm(realm)
                                                 .users()
                                                 .searchByUsername(username, true);

        if (users.isEmpty()) {
            log.info("User with username: {} not found in realm: {}", username, realm);
            return;
        }

        if (users.size() > 1) {
            log.warn("Multiple users found with username: {} in realm: {}. Using the first match", username, realm);
        }

        String userId = users.get(0).getId();
        try (Response response = keycloak.realm(realm).users().delete(userId)) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                String body;
                try {
                    body = response.readEntity(String.class);
                } catch (Exception ignored) {
                    body = "<unable to read response body>";
                }

                String message = String.format(
                    "Failed to delete user %s from Keycloak. Status: %d %s. Response body: %s",
                    username,
                    response.getStatus(),
                    response.getStatusInfo().getReasonPhrase(),
                    body
                );

                throw new RuntimeException(message);
            }
        }
    }
}
