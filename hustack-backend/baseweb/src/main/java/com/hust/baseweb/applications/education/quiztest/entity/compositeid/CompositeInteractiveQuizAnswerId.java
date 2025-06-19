package com.hust.baseweb.applications.education.quiztest.entity.compositeid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompositeInteractiveQuizAnswerId implements Serializable {
    private UUID interactiveQuizId;
    private UUID questionId;
    private String userId;
    private UUID choiceAnswerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositeInteractiveQuizAnswerId that = (CompositeInteractiveQuizAnswerId) o;
        return Objects.equals(interactiveQuizId, that.interactiveQuizId) &&
               Objects.equals(questionId, that.questionId) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(choiceAnswerId, that.choiceAnswerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interactiveQuizId, questionId, userId, choiceAnswerId);
    }

}
