package com.hust.baseweb.applications.exam.model.response;

import java.time.LocalDateTime;

public interface ExamQuestionDetailsRes {
    String getId();
    String getCode();
    Integer getType();
    String getLevel();
    String getContent();
    String getFilePath();
    Integer getNumberAnswer();
    String getContentAnswer1();
    String getContentAnswer2();
    String getContentAnswer3();
    String getContentAnswer4();
    String getContentAnswer5();
    String getContentFileAnswer1();
    String getContentFileAnswer2();
    String getContentFileAnswer3();
    String getContentFileAnswer4();
    String getContentFileAnswer5();
    boolean getMultichoice();
    String getAnswer();
    String getExplain();
    String getCreatedBy();
    LocalDateTime getCreatedAt();
    String getUpdatedBy();
    LocalDateTime getUpdatedAt();
    String getExamSubjectName();
    String getExamTagIdStr();
    String getExamTagNameStr();
}
