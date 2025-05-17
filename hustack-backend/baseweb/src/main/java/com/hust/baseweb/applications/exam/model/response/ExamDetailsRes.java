package com.hust.baseweb.applications.exam.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

public interface ExamDetailsRes {
    String getId();
    String getCode();
    String getName();
    String getDescription();
    Integer getStatus();
    String getAnswerStatus();
    String getStartTime();
    String getEndTime();
    @JsonRawValue
    String getExamTests();
}
