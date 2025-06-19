package com.hust.baseweb.applications.exam.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import jakarta.persistence.Column;

@Getter
@Setter
@FieldNameConstants
public class ExamQuestionTagSaveReq {

    private String examTagId;
    private String examQuestionId;
}
