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
    Integer getTotalTime();
    Integer getTotalViolate();
    String getSubmitedAt();
}
