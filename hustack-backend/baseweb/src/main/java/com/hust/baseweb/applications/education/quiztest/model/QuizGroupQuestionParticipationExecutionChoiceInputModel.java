package com.hust.baseweb.applications.education.quiztest.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class QuizGroupQuestionParticipationExecutionChoiceInputModel {

    @NotNull(message = "Được yêu cầu")
    private String testId;

    @NotNull(message = "Được yêu cầu")
    private UUID questionId;

    @NotNull(message = "Được yêu cầu")
    private UUID quizGroupId;

    @NotNull(message = "Được yêu cầu")
    //@Size(min = 1, message = "Yêu cầu ít nhất là 1 phần tử")
    @Size(min = 0, message = "Yêu cầu ít nhất là 0 phần tử")
    private List<UUID> chooseAnsIds;

}
