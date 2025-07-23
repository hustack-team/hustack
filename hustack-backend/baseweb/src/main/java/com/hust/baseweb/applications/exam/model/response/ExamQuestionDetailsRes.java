package com.hust.baseweb.applications.exam.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;

public interface ExamQuestionDetailsRes {
    String getId();
    String getCode();
    Integer getType();
    String getLevel();
    String getContent();
    String getFilePath();
    Integer getNumberAnswer();
    boolean getMultichoice();
    String getAnswer();
    String getExplain();
    String getCreatedBy();
    LocalDateTime getCreatedAt();
    String getUpdatedBy();
    LocalDateTime getUpdatedAt();
    String getExamSubjectName();
    @JsonRawValue
    String getExamTags();
    @JsonRawValue
    String getAnswers();
}
