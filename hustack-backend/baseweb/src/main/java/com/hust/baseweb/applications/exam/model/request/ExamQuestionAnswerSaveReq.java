package com.hust.baseweb.applications.exam.model.request;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class ExamQuestionAnswerSaveReq {

    private String id;
    private String examQuestionId;
    private Integer order;
    private String content;
    private String file;
}
