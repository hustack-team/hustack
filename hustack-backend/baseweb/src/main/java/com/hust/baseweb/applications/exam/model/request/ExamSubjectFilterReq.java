package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.utils.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
public class ExamSubjectFilterReq {

    private String keyword;
    private Constants.Status status;
}
