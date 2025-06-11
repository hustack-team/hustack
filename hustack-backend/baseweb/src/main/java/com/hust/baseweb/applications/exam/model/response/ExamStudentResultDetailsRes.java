package com.hust.baseweb.applications.exam.model.response;

public interface ExamStudentResultDetailsRes {

    String getExamStudentTestId();
    String getId();
    String getCode();
    String getName();
    String getEmail();
    String getPhone();
    String getExamResultId();
    Boolean getSubmitAgain();
    Float getTotalScore();
    String getStartedAt();
    Integer getTotalViolate();
    String getSubmitedAt();
}
