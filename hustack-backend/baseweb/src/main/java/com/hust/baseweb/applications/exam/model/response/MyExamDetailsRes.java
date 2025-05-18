package com.hust.baseweb.applications.exam.model.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.List;

@Getter
@Setter
public class MyExamDetailsRes {

    private String examStudentTestId;
    private String examId;
    private String examAnswerStatus;
    private String examCode;
    private String examName;
    private String examDescription;
    private String startTime;
    private String endTime;
    private String examTestId;
    private String examTestCode;
    private String examTestName;
    private String examResultId;
    private Float totalScore;
    private Integer totalTime;
    private String submitedAt;
    private String answerFiles;
    private String comment;
    private List<MyExamQuestionDetailsRes> questionList;

    public MyExamDetailsRes(MyExamDetailsResDB myExamDetailsResDB,
                            List<MyExamQuestionDetailsRes> questionList,
                            ModelMapper modelMapper) {
        modelMapper.map(myExamDetailsResDB, this);
        this.questionList = questionList;
    }
}
