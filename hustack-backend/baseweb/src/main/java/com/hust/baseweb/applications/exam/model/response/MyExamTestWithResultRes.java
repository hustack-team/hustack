package com.hust.baseweb.applications.exam.model.response;

public interface MyExamTestWithResultRes {
    String getExamStudentTestId();
    String getExamTestId();
    String getExamTestCode();
    String getExamTestName();
    String getExamTestDescription();
    Integer getTotalScore();
    Integer getTotalTime();
}
