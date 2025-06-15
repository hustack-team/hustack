package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "contest_submission_block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestSubmissionBlock {

    @Id
    @GeneratedValue(generator = "uuid1")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "block_seq")
    private Integer blockSeq;

    @Column(name = "source_code", columnDefinition = "text")
    private String sourceCode;
}
