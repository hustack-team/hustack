package com.hust.baseweb.applications.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "exam_test_question")
public class ExamTestQuestionEntity {

    @Id
    @Column(length = 60)
    protected String id;

    @Column(name = "exam_test_id")
    private String examTestId;

    @Column(name = "exam_question_id")
    private String examQuestionId;

    @Column(name = "\"order\"")
    private Integer order;
}
