package com.hust.baseweb.applications.exam.model.response;

import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.List;

@Getter
@Setter
public class ExamMarkingDetailsRes {

    private String examId;
    private String examStudentTestId;
    private String examStudentId;
    private String examStudentCode;
    private String examStudentName;
    private String examStudentEmail;
    private String examStudentPhone;
    private String examResultId;
    private Float totalScore;
    private Integer totalTime;
    private String submitedAt;
    private String answerFiles;
    private String comment;
    private List<MyExamQuestionDetailsRes> questionList;

    public ExamMarkingDetailsRes(ExamMarkingDetailsResDB examMarkingDetailsResDB,
                                 List<MyExamQuestionDetailsRes> questionList,
                                 ModelMapper modelMapper){
        modelMapper.map(examMarkingDetailsResDB, this);
        this.questionList = questionList;
    }
}
