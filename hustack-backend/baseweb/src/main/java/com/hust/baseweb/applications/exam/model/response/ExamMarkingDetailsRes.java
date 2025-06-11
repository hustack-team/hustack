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
public class ExamMarkingDetailsRes {

    String examId;
    String examStudentTestId;
    String examStudentId;
    String examStudentCode;
    String examStudentName;
    String examStudentEmail;
    String examStudentPhone;
    String examResultId;
    Float totalScore;
    String startedAt;
    String submitedAt;
    String answerFiles;
    String comment;
    List<MyExamQuestionDetailsRes> questionList;

    public ExamMarkingDetailsRes(ExamMarkingDetailsResDB examMarkingDetailsResDB,
                                 List<MyExamQuestionDetailsRes> questionList,
                                 ModelMapper modelMapper){
        modelMapper.map(examMarkingDetailsResDB, this);
        this.questionList = questionList;
    }
}
