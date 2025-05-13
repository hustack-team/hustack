package com.hust.baseweb.applications.programmingcontest.entity;

import com.hust.baseweb.applications.programmingcontest.model.ContestSubmission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "contest_submission_block")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestSubmissionBlock {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private ContestSubmission submission;

    @Column(name = "block_seq")
    private Integer blockSeq;

    @Column(name = "source_code", columnDefinition = "text")
    private String sourceCode;
}
