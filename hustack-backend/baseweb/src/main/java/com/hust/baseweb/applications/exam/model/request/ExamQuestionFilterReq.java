package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
public class ExamQuestionFilterReq {

    private String keyword;
    private Integer type;
    private String level;
    private String examSubjectId;
    private String examTagIds;
}
