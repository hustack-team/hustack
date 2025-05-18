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
public class ExamMarkingSaveReq {

    String examResultId;
    String comment;
    Float totalScore;
    List<String> commentFilePathDeletes;
    List<ExamMarkingDetailsSaveReq> examResultDetails;
}
