package com.hust.baseweb.applications.education.quiztest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "edu_test_quiz_group")
public class EduTestQuizGroup {

    @Id
    @Column(name = "quiz_group_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID quizGroupId;

    @Column(name = "test_id")
    private String testId;

    @Column(name = "group_code")
    private String groupCode;

    @Column(name = "note")
    private String note;
}
