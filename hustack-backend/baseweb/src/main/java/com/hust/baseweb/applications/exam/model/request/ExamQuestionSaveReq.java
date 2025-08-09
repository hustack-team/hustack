package com.hust.baseweb.applications.exam.model.request;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
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
public class ExamQuestionSaveReq {

    String examSubjectId;
    String code;
    Integer type;
    String level;
    String content;
    String filePath;
    Integer numberAnswer;
    List<ExamQuestionAnswerSaveReq> answers;
    List<ExamQuestionAnswerSaveReq> answersDelete;
    boolean multichoice;
    String answer;
    String explain;
    List<String> deletePaths;
    List<ExamTagEntity> examTags;
}
