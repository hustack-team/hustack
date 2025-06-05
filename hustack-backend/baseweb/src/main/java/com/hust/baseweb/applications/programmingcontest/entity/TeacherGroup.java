package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher_group")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherGroup {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "created_by", length = 60)
    String createdBy;

    @Column(name = "name", length = 100)
    String name;

//    @Column(name = "status", length = 100)
//    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by", length = 60)
    private String lastModifiedBy;

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
