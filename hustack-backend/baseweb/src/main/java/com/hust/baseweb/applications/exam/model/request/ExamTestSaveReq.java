package com.hust.baseweb.applications.exam.model.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamTestSaveReq {

    String code;
    String name;
    String description;
    List<ExamTestQuestionSaveReq> examTestQuestionSaveReqList;
    List<ExamTestQuestionSaveReq> examTestQuestionDeleteReqList;
}
