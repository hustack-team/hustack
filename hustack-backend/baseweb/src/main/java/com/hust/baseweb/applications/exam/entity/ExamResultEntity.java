package com.hust.baseweb.applications.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "exam_result")
public class ExamResultEntity{

    @Id
    @Column(length = 60)
    protected String id;

    @Column(name = "exam_student_test_id")
    private String examStudentTestId;

    @Column(name = "total_score")
    private Float totalScore;

    @Column(name = "total_time")
    private Integer totalTime;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "comment")
    private String comment;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submited_at")
    private LocalDateTime submitedAt;

    @Column(name = "submit_again")
    private Boolean submitAgain;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID().toString();
        startedAt = LocalDateTime.now();
    }
}
