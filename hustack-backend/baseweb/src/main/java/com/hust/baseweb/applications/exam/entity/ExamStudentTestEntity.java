package com.hust.baseweb.applications.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "exam_student_test")
public class ExamStudentTestEntity {

    @Id
    @Column(length = 60)
    protected String id;

    @Column(name = "exam_student_id")
    private String examStudentId;

    @Column(name = "exam_exam_test_id")
    private String examExamTestId;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID().toString();
    }
}
