package com.hust.baseweb.applications.exam.service.impl;

import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamResultUpdateReq;
import com.hust.baseweb.applications.exam.repository.ExamResultRepository;
import com.hust.baseweb.applications.exam.service.ExamResultService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamResultServiceImpl implements ExamResultService {

    ExamResultRepository examResultRepository;
    ModelMapper modelMapper;


    @Override
    public ResponseData<ExamResultEntity> update(ExamResultUpdateReq examResultUpdateReq) {
        ResponseData<ExamResultEntity> responseData = new ResponseData<>();

        Optional<ExamResultEntity> examResultExist = examResultRepository.findByExamStudentTestId(examResultUpdateReq.getExamStudentTestId());
        if(examResultExist.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn bài làm");
            return responseData;
        }

        ExamResultEntity examResultEntity = examResultExist.get();
        examResultEntity.setSubmitAgain(examResultUpdateReq.getSubmitAgain());
        examResultEntity.setExtraTime(examResultUpdateReq.getExtraTime());
        examResultRepository.save(examResultEntity);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg(Boolean.TRUE.equals(examResultUpdateReq.getSubmitAgain()) ? "Mở thành công cho thí sinh tiếp tục làm bài thi!" : "Success");
        return responseData;
    }
}
