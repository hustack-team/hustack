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
@Table(name = "exam_exam_test")
public class ExamExamTestEntity {

    @Id
    @Column(length = 60)
    protected String id;

    @Column(name = "exam_id")
    private String examId;

    @Column(name = "exam_test_id")
    private String examTestId;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID().toString();
    }
}
