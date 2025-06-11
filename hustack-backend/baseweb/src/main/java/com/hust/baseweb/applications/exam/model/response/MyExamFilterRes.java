package com.hust.baseweb.applications.exam.model.response;

public interface MyExamFilterRes {

    String getExamId();
    String getExamCode();
    String getExamName();
    String getExamDescription();
    Integer getExamMonitor();
    Integer getExamBlockScreen();
    String getStartTime();
    String getEndTime();
}
