package com.hust.baseweb.applications.education.quiztest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "history_log_quiz_group_question_participation_execution_choice")
public class HistoryLogQuizGroupQuestionParticipationExecutionChoice {

    @Id
    @Column(name = "log_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID logId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "quiz_group_id")
    private UUID quizGroupId;

    @Column(name = "choice_answer_id")
    private UUID choiceAnswerId;

    @Column(name = "participation_user_login_id")
    private String participationUserLoginId;

    @Column(name = "created_stamp")
    private Date createdStamp;
}
