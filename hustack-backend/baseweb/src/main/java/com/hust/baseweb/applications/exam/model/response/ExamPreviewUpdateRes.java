package com.hust.baseweb.applications.exam.model.response;

import com.hust.baseweb.applications.exam.entity.ExamStudentEntity;
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
public class ExamPreviewUpdateRes {

    String id;
    String code;
    String name;
    String description;
    Integer status;
    String answerStatus;
    Integer scoreStatus;
    Integer monitor;
    Integer blockScreen;
    String startTime;
    String endTime;
    List<ExamExamTestPreviewUpdateRes> examExamTests;
    List<ExamStudentEntity> examStudents;
}
