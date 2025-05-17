package com.hust.baseweb.applications.exam.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

public interface MyExamFilterRes {

    String getExamId();
    String getExamCode();
    String getExamName();
    String getExamDescription();
    String getStartTime();
    String getEndTime();
    @JsonRawValue
    String getExamTestIds();

}
