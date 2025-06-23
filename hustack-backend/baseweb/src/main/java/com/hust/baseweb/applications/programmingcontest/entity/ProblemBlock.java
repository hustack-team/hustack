package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "contest_problem_block")
@FieldDefaults(level = AccessLevel.PRIVATE)
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
}
