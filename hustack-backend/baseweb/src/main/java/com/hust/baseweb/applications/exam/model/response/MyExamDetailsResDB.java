package com.hust.baseweb.applications.exam.model.response;

public interface MyExamDetailsResDB {
    String getExamStudentTestId();
    String getExamId();
    String getExamAnswerStatus();
    String getExamCode();
    String getExamName();
    String getExamDescription();
    String getExamMonitor();
    String getExamBlockScreen();
    String getStartTime();
    String getEndTime();
    String getExamTestId();
    String getExamTestCode();
    String getExamTestName();
    String getExamTestDuration();
    String getExamResultId();
    Boolean getSubmitAgain();
    Float getTotalScore();
    Integer getTotalTime();
    String getSubmitedAt();
    String getAnswerFiles();
    String getComment();

}
