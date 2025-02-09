package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
public class ExamQuestionSaveReq {

    private String examSubjectId;
    private String code;
    private Integer type;
    private String level;
    private String content;
    private String filePath;
    private Integer numberAnswer;
    private String contentAnswer1;
    private String contentAnswer2;
    private String contentAnswer3;
    private String contentAnswer4;
    private String contentAnswer5;
    private boolean multichoice;
    private String answer;
    private String explain;
    private List<String> deletePaths;
    private List<ExamTagEntity> examTags;
}
