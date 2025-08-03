package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "contest_problem_block")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class ProblemBlock {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id;

    @Column(name = "problem_id")
    String problemId;

    @Column(name = "seq")
    int seq;

    @Column(name = "completed_by")
    int completedBy; // 0 for teacher, 1 for student

    @Column(name = "source_code")
    String sourceCode;

    @Column(name = "programming_language")
    String programmingLanguage;

    @CreatedBy
    @Column(name = "created_by")
    String createdBy;

    @CreatedDate
    @Column(name = "created_date")
    java.util.Date createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    java.util.Date lastModifiedDate;
}
