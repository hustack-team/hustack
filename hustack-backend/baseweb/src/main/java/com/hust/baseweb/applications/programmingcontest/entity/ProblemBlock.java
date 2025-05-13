package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "contest_problem_block")
public class ProblemBlock {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "problem_id")
    private String problemId;

    @Column(name = "seq")
    private int seq;

    @Column(name = "completed_by")
    private String completedBy;

    @Column(name = "source_code")
    private String sourceCode;

    @Column(name = "programming_language")
    private String programmingLanguage;
}
