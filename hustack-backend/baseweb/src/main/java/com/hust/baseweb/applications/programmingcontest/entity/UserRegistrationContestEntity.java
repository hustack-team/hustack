package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "user_registration_contest_new")
public class UserRegistrationContestEntity {

    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_PARTICIPANT = "PARTICIPANT";
    public static final String ROLE_OWNER = "OWNER";

    public static final String STATUS_SUCCESSFUL = "SUCCESSFUL";
    public static final String STATUS_PENDING = "PENDING";

    public static final String PERMISSION_SUBMIT = "SUBMIT";
    public static final String PERMISSION_FORBIDDEN_SUBMIT = "FORBIDDEN_SUBMIT";

    public static List<String> getListPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(UserRegistrationContestEntity.PERMISSION_SUBMIT);
        permissions.add(UserRegistrationContestEntity.PERMISSION_FORBIDDEN_SUBMIT);

        return permissions;
    }

    public static List<String> getListRoles() {
        List<String> roles = new ArrayList<>();
        roles.add(ROLE_MANAGER);
        roles.add(ROLE_PARTICIPANT);
        roles.add(ROLE_OWNER);

        return roles;
    }

    @Id
    @Column(name = "user_registration_contest_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

//    @JoinColumn(name = "user_id", referencedColumnName = "user_login_id")
//    @ManyToOne(fetch = FetchType.LAZY)
//    private UserLogin userLogin;

    @Column(name = "user_id")
    private String userId;

//    @JoinColumn(name = "contest_id", referencedColumnName = "contest_id")
//    @ManyToOne(fetch = FetchType.LAZY)
//    private ContestEntity contest;

    @Column(name = "contest_id")
    private String contestId;

    @Column(name = "status")
    private String status;

    @Column(name = "role_id")
    private String roleId;

    @Column(name = "created_date")
    private Date createdStamp;

    @Column(name = "last_updated")
    private Date lastUpdated;

    @Column(name = "updated_by_user_login_id")
    private String updatedByUserLogin_id;

    @Column(name = "permission_id")
    private String permissionId;

    @Column(name = "fullname")
    private String fullname;
}
