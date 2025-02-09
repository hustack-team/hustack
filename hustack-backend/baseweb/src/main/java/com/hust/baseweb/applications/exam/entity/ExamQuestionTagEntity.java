package com.hust.baseweb.applications.exam.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "exam_question_tag")
public class ExamQuestionTagEntity {

    @EmbeddedId
    private ExamQuestionTagKey id;
}
