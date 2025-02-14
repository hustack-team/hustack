package com.hust.baseweb.applications.exam.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ExamQuestionTagKey implements Serializable {

    @Column(name = "exam_tag_id")
    private String examTagId;

    @Column(name = "exam_question_id")
    private String examQuestionId;
}
