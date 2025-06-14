package com.hust.baseweb.service;

import com.hust.baseweb.config.KeycloakAdminProperties;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.tika.utils.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

        // Create user
        String realm = keycloakAdminProperties.getRealm();
        Response response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
//                credential.setTemporary(true); // Require password change on first login
            credential.setValue(password);
            keycloak.realm(realm).users().get(userId).resetPassword(credential);

            // Assign group
            String studentGroupId = getStudentGroupId();
            keycloak.realm(realm).users().get(userId).joinGroup(studentGroupId);

            result = true;
        } else {
            result = false;
        }

        response.close();

        return result;
    }

    public void updateEnabledUser(String username, boolean enabled) {
        if (!StringUtils.isBlank(username)) {
            String realm = keycloakAdminProperties.getRealm();
            List<UserRepresentation> users = keycloak.realm(realm)
                                                     .users()
                                                     .search(username, true);

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

    public void resetPassword(String username, String newPassword) {
        String realm = keycloakAdminProperties.getRealm();
        List<UserRepresentation> users = keycloak.realm(realm)
                                                 .users()
                                                 .search(username, true);
        if (users.isEmpty()) {
            throw new RuntimeException("User not found with username: " + username + " in realm: " + realm);
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
}
