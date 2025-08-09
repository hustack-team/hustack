package com.hust.baseweb.applications.exam.model.response;

public interface MyExamTestWithResultRes {
    String getExamStudentTestId();
    String getExamTestId();
    String getExamTestCode();
    String getExamTestName();
    Integer getExamTestDuration();
    String getExamTestDescription();
    String getExamResultId();
    Float getTotalScore();
    String getStartedAt();
    String getSubmitedAt();
    Boolean getSubmitAgain();
    Integer getTotalViolate();
}
