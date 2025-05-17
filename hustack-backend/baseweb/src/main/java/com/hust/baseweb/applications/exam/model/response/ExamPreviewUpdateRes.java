package com.hust.baseweb.applications.exam.model.response;

import com.hust.baseweb.applications.exam.entity.ExamStudentEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
public class ExamPreviewUpdateRes {

    private String id;
    private String code;
    private String name;
    private String description;
    private Integer status;
    private String answerStatus;
    private String startTime;
    private String endTime;
    private List<ExamExamTestPreviewUpdateRes> examExamTests;
    private List<ExamStudentEntity> examStudents;
}
