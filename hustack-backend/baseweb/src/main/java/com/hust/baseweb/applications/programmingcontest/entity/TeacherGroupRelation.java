package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher_group_relation")
@IdClass(TeacherGroupRelationId.class)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherGroupRelation {

    @Id
    @Column(name = "group_id")
    UUID groupId;

    @Id
    @Column(name = "user_id")
    String userId;

    @Column(name = "last_modified_date")
    LocalDateTime lastModifiedDate;

    @Column(name = "created_date")
    LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastModifiedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }
}
