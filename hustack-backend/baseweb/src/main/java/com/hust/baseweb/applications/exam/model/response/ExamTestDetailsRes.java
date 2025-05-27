package com.hust.baseweb.applications.exam.model.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamTestDetailsRes {

    String id;
    String code;
    String name;
    Integer duration;
    String description;
    List<ExamTestQuestionDetailsRes> examTestQuestionDetails;
}
