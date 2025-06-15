package com.hust.baseweb.applications.exam.model.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamFilterReq {

    String keyword;
    Integer status;
    String startTimeFrom;
    String startTimeTo;
    String endTimeFrom;
    String endTimeTo;
}
