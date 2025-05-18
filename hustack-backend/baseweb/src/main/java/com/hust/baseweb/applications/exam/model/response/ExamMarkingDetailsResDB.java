package com.hust.baseweb.applications.exam.model.response;

public interface ExamMarkingDetailsResDB {
    String getExamId();
    String getExamStudentTestId();
    String getExamStudentId();
    String getExamStudentCode();
    String getExamStudentName();
    String getExamStudentEmail();
    String getExamStudentPhone();
    String getExamResultId();
    Float getTotalScore();
    Integer getTotalTime();
    String getSubmitedAt();
    String getAnswerFiles();
    String getComment();

}
