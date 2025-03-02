package com.hust.baseweb.applications.exam.model.response;

public interface MyExamDetailsResDB {
    String getExamStudentId();
    String getExamId();
    String getExamAnswerStatus();
    String getExamCode();
    String getExamName();
    String getExamDescription();
    String getStartTime();
    String getEndTime();
    String getExamTestId();
    String getExamTestCode();
    String getExamTestName();
    String getExamResultId();
    Integer getTotalScore();
    Integer getTotalTime();
    String getSubmitedAt();
    String getAnswerFiles();
    String getComment();

}
