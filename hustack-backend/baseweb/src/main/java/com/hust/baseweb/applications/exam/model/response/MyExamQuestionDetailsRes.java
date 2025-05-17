package com.hust.baseweb.applications.exam.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

public interface MyExamQuestionDetailsRes {

    String getExamTestQuestionId();
    String getExamResultDetailsId();
    String getQuestionId();
    String getQuestionCode();
    Integer getQuestionType();
    String getQuestionContent();
    String getQuestionFile();
    Integer getQuestionNumberAnswer();
    boolean getQuestionMultichoice();
    String getQuestionAnswer();
    String getQuestionExplain();
    Integer getQuestionOrder();
    String getAnswer();
    String getFilePathAnswer();
    String getFilePathComment();
    Integer getScore();
    @JsonRawValue
    String getQuestionAnswers();
}
