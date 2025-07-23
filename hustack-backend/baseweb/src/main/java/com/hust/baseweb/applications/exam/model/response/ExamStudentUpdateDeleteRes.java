package com.hust.baseweb.applications.exam.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

public interface ExamStudentUpdateDeleteRes {
    String getId();
    @JsonRawValue
    String getExamStudentTests();
}
