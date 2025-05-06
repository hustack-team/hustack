package com.hust.baseweb.applications.exam.model.response;

public interface MyExamQuestionDetailsRes {

    String getExamTestQuestionId();
    String getExamResultDetailsId();
    String getQuestionId();
    String getQuestionCode();
    Integer getQuestionType();
    String getQuestionContent();
    String getQuestionFile();
    Integer getQuestionNumberAnswer();
    String getQuestionContentAnswer1();
    String getQuestionContentAnswer2();
    String getQuestionContentAnswer3();
    String getQuestionContentAnswer4();
    String getQuestionContentAnswer5();
    String getQuestionContentFileAnswer1();
    String getQuestionContentFileAnswer2();
    String getQuestionContentFileAnswer3();
    String getQuestionContentFileAnswer4();
    String getQuestionContentFileAnswer5();
    boolean getQuestionMultichoice();
    String getQuestionAnswer();
    String getQuestionExplain();
    Integer getQuestionOrder();
    String getAnswer();
    String getFilePathAnswer();
    String getFilePathComment();
    Integer getScore();
}
