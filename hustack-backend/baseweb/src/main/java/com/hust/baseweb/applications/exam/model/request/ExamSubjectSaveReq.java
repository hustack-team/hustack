package com.hust.baseweb.applications.exam.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class ExamSubjectSaveReq {

    private String code;
    private String name;
    private String status;
}
