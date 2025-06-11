package com.hust.baseweb.applications.exam.model.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamTestFilterRes {

    String id;
    String code;
    String name;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<ExamTestQuestionFilterRes> examTestQuestionFilterResList;
}
