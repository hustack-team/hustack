package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teacher_group")
@Data
public class TeacherGroup {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_by_user_id", length = 60)
    private String createdByUserId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
