package com.hust.baseweb.applications.exam.model.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamTestQuestionFilterRes {

    String id;
    String examTestId;
    String examQuestionId;
    Integer order;
}
