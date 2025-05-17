package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.entity.ExamStudentTestEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
public class ExamStudentSaveReq {

    private String id;
    private String code;
    private String name;
    private String email;
    private String phone;
    private List<ExamStudentTestEntity> examStudentTests;
}
