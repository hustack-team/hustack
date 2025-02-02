package com.hust.baseweb.applications.education.model.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommentOnQuizQuestionIM {

    private String comment;
    private UUID questionId;
    private UUID replyToCommentId;
}
