package com.hust.baseweb.applications.exam.model.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Getter
@Setter
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamMonitorSaveReq {

    String examResultId;
    Integer platform;
    Integer type;
    String startTime;
    String toTime;
    String note;
}
