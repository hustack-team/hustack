package com.hust.baseweb.applications.exam.model.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.modelmapper.ModelMapper;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyExamDetailsRes {

    String examStudentTestId;
    String examId;
    String examAnswerStatus;
    String examCode;
    String examName;
    String examDescription;
    Integer examMonitor;
    Integer examBlockScreen;
    String startTime;
    String endTime;
    String examTestId;
    String examTestCode;
    String examTestName;
    Integer examTestDuration;
    String examResultId;
    String startedAt;
    Boolean submitAgain;
    Float totalScore;
    String submitedAt;
    String answerFiles;
    String comment;
    List<MyExamQuestionDetailsRes> questionList;

    public MyExamDetailsRes(MyExamDetailsResDB myExamDetailsResDB,
                            List<MyExamQuestionDetailsRes> questionList,
                            ModelMapper modelMapper) {
        modelMapper.map(myExamDetailsResDB, this);
        this.questionList = questionList;
    }
}
