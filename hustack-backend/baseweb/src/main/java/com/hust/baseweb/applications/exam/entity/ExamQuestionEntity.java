package com.hust.baseweb.applications.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "exam_question")
public class ExamQuestionEntity extends BaseEntity {

    @Column(name = "exam_subject_id")
    private String examSubjectId;

    @Column(name = "code")
    private String code;

    @Column(name = "type")
    private Integer type;

    @Column(name = "level")
    private String level;

    @Column(name = "content")
    private String content;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "number_answer")
    private Integer numberAnswer;

    @Column(name = "multichoice")
    private boolean multichoice;

    @Column(name = "answer")
    private String answer;

    @Column(name = "explain")
    private String explain;
}
