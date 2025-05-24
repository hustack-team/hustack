package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.entity.ExamExamTestEntity;
import com.hust.baseweb.applications.exam.entity.ExamStudentEntity;
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
public class ExamSaveReq {

    String code;
    String name;
    String description;
    Integer status;
    String answerStatus;
    String startTime;
    String endTime;
    List<ExamExamTestEntity> examExamTests;
    List<ExamExamTestEntity> examExamTestDeletes;
    List<ExamStudentSaveReq> examStudents;
    List<ExamStudentSaveReq> examStudentDeletes;
}
