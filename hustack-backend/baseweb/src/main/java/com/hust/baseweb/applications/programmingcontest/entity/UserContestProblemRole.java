package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "user_contest_problem_role")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserContestProblemRole {

    public static final String ROLE_EDITOR = "EDITOR";

    public static final String ROLE_VIEWER = "VIEWER";

    public static final String ROLE_OWNER = "OWNER";

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id;

    @Column(name = "user_id")
    String userId;

    @Column(name = "problem_id")
    String problemId;

    @Column(name = "role_id")
    String roleId;

    @CreatedBy
    @Column(name = "created_by")
    String createdBy;

    @LastModifiedBy
    @Column(name = "update_by_user_id")
    String lastModifiedBy;

    @CreatedDate
    @Column(name = "created_stamp")
    Date createdDate;

    @LastModifiedDate
    @Column(name = "last_updated_stamp")
    Date lastModifiedDate;

}
