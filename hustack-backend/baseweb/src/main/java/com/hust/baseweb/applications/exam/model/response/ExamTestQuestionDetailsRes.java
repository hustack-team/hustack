package com.hust.baseweb.applications.exam.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

public interface ExamTestQuestionDetailsRes {

    String getExamTestQuestionId();
    String getQuestionId();
    String getQuestionCode();
    Integer getQuestionType();
    String getQuestionLevel();
    String getQuestionContent();
    String getQuestionFile();
    Integer getQuestionNumberAnswer();
    boolean getQuestionMultichoice();
    String getQuestionAnswer();
    String getQuestionExplain();
    Integer getQuestionOrder();
    String getExamSubjectName();
    @JsonRawValue
    String getQuestionExamTags();
    @JsonRawValue
    String getQuestionAnswers();
}
