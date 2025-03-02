package com.hust.baseweb.applications.exam.model.response;

public interface ExamMarkingDetailsResDB {
    String getExamId();
    String getExamTestId();
    String getExamStudentId();
    String getExamStudentCode();
    String getExamStudentName();
    String getExamStudentEmail();
    String getExamStudentPhone();
    String getExamResultId();
    Integer getTotalScore();
    Integer getTotalTime();
    String getSubmitedAt();
    String getAnswerFiles();
    String getComment();

}
