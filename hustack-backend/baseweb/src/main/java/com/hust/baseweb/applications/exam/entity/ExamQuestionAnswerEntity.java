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
@Table(name = "exam_question_answer")
public class ExamQuestionAnswerEntity {

    @Id
    @Column(length = 60)
    protected String id;

    @Column(name = "exam_question_id")
    private String examQuestionId;

    @Column(name = "\"order\"")
    private Integer order;

    @Column(name = "content")
    private String content;

    @Column(name = "file")
    private String file;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID().toString();
    }
}
