package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.entity.ExamStudentTestEntity;
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
public class ExamStudentSaveReq {

    String id;
    String code;
    String name;
    String email;
    String phone;
    List<ExamStudentTestEntity> examStudentTests;
}
