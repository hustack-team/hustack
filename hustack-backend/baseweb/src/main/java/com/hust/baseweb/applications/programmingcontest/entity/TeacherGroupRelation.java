package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher_group_relation")
@IdClass(TeacherGroupRelationId.class)
@Data
public class TeacherGroupRelation {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_PARTICIPANT = "PARTICIPANT";

    @Id
    @Column(name = "group_id")
    private UUID groupId;

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "last_updated_stamp")
    private LocalDateTime lastUpdatedStamp;

    @Column(name = "created_stamp")
    private LocalDateTime createdStamp;

    @PrePersist
    protected void onCreate() {
        createdStamp = LocalDateTime.now();
        lastUpdatedStamp = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedStamp = LocalDateTime.now();
    }
}
