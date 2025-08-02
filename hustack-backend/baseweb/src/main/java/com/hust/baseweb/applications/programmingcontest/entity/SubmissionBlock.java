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

@Entity
@Table(name = "contest_submission_block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class SubmissionBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "submission_id")
    UUID submissionId;

    @Column(name = "block_id")
    UUID blockId;

    @Column(name = "source_code", columnDefinition = "text")
    String sourceCode;

    @CreatedBy
    @Column(name = "created_by")
    String createdBy;

    @CreatedDate
    @Column(name = "created_date")
    Date createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    Date lastModifiedDate;
}
