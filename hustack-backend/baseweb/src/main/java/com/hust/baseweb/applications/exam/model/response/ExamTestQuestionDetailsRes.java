package com.hust.baseweb.applications.exam.model.response;

public interface ExamTestQuestionDetailsRes {

    String getExamTestQuestionId();
    String getQuestionId();
    String getQuestionCode();
    Integer getQuestionType();
    String getQuestionLevel();
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
    String getExamSubjectName();
    String getExamTagIdStr();
    String getExamTagNameStr();
}
