package com.hust.baseweb.applications.exam.entity;

import lombok.*;

import jakarta.persistence.*;
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
