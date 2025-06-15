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
    Integer getExamTestDuration();
    Integer getExamTestExtraTime();
    String getExamResultId();
    String getStartedAt();
    Boolean getSubmitAgain();
    Float getTotalScore();
    String getSubmitedAt();
    String getAnswerFiles();
    String getComment();

}
