package com.hust.baseweb.applications.exam.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.exam.entity.ExamEntity;
import com.hust.baseweb.applications.exam.entity.ExamTestEntity;
import com.hust.baseweb.applications.exam.entity.ExamTestQuestionEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.ExamTestDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamTestQuestionDetailsRes;
import com.hust.baseweb.applications.exam.repository.ExamRepository;
import com.hust.baseweb.applications.exam.repository.ExamTestQuestionRepository;
import com.hust.baseweb.applications.exam.repository.ExamTestRepository;
import com.hust.baseweb.applications.exam.service.ExamTestService;
import com.hust.baseweb.applications.exam.utils.DataUtils;
import com.hust.baseweb.applications.exam.utils.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamTestServiceImpl implements ExamTestService {

    ExamRepository examRepository;
    ExamTestRepository examTestRepository;
    ExamTestQuestionRepository examTestQuestionRepository;
    EntityManager entityManager;
    ModelMapper modelMapper;
    ObjectMapper objectMapper;

    @Override
    public Page<ExamTestEntity> filter(Pageable pageable, ExamTestFilterReq examTestFilterReq) {
        return examTestRepository.filter(
            pageable,
            SecurityUtils.getUserLogin(),
            DataUtils.formatStringValueSqlToLocalDateTime(examTestFilterReq.getCreatedFrom(), true),
            DataUtils.formatStringValueSqlToLocalDateTime(examTestFilterReq.getCreatedTo(), false),
            examTestFilterReq.getKeyword()
        );
    }

    @Override
    public ResponseData<ExamTestDetailsRes> details(String id) {
        ResponseData<ExamTestDetailsRes> responseData = new ResponseData<>();

        Optional<ExamTestEntity> examTestEntity = examTestRepository.findById(id);
        if(!examTestEntity.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại đề thi");
            return responseData;
        }

        List<ExamTestQuestionDetailsRes> list = examTestRepository.details(SecurityUtils.getUserLogin(), id);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Success");
        responseData.setData(ExamTestDetailsRes.builder()
                                 .id(examTestEntity.get().getId())
                                 .code(examTestEntity.get().getCode())
                                 .name(examTestEntity.get().getName())
                                 .description(examTestEntity.get().getDescription())
                                 .examTestQuestionDetails(list).build());
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamTestEntity> create(ExamTestSaveReq examTestSaveReq) {
        ResponseData<ExamTestEntity> responseData = new ResponseData<>();

        Optional<ExamTestEntity> examTestExist = examTestRepository.findByCode(examTestSaveReq.getCode());
        if(examTestExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.ALREADY_REPORTED);
            responseData.setResultCode(HttpStatus.ALREADY_REPORTED.value());
            responseData.setResultMsg("Đã tồn tại mã đề thi");
            return responseData;
        }

        ExamTestEntity examTestEntity = modelMapper.map(examTestSaveReq, ExamTestEntity.class);
        examTestEntity = examTestRepository.save(examTestEntity);

        List<ExamTestQuestionEntity> examTestQuestionEntityList = new ArrayList<>();
        for(ExamTestQuestionSaveReq examTestQuestionSaveReq: examTestSaveReq.getExamTestQuestionSaveReqList()){
            ExamTestQuestionEntity examTestQuestionEntity = modelMapper.map(examTestQuestionSaveReq, ExamTestQuestionEntity.class);
            examTestQuestionEntity.setId(UUID.randomUUID().toString());
            examTestQuestionEntity.setExamTestId(examTestEntity.getId());
            examTestQuestionEntityList.add(examTestQuestionEntity);
        }
        examTestQuestionRepository.saveAll(examTestQuestionEntityList);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Thêm mới đề thi thành công");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamTestEntity> update(ExamTestSaveReq examTestSaveReq) {
        ResponseData<ExamTestEntity> responseData = new ResponseData<>();

        Optional<ExamTestEntity> examTestExist = examTestRepository.findByCode(examTestSaveReq.getCode());
        if(!examTestExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại đề thi");
            return responseData;
        }

        ExamTestEntity examTestEntity = modelMapper.map(examTestSaveReq, ExamTestEntity.class);
        examTestEntity.setId(examTestExist.get().getId());
        examTestEntity.setCreatedBy(examTestExist.get().getCreatedBy());
        examTestEntity.setCreatedAt(examTestExist.get().getCreatedAt());
        examTestRepository.save(examTestEntity);

        List<ExamTestQuestionEntity> examTestQuestionEntityList = new ArrayList<>();
        for(ExamTestQuestionSaveReq examTestQuestionSaveReq: examTestSaveReq.getExamTestQuestionSaveReqList()){
            ExamTestQuestionEntity examTestQuestionEntity = modelMapper.map(examTestQuestionSaveReq, ExamTestQuestionEntity.class);
            if(!StringUtils.isNotEmpty(examTestQuestionEntity.getId())){
                examTestQuestionEntity.setId(UUID.randomUUID().toString());
            }
            examTestQuestionEntity.setExamTestId(examTestEntity.getId());
            examTestQuestionEntityList.add(examTestQuestionEntity);
        }
        examTestQuestionRepository.saveAll(examTestQuestionEntityList);

        List<ExamTestQuestionEntity> examTestQuestionDeleteList = new ArrayList<>();
        for(ExamTestQuestionSaveReq examTestQuestion: examTestSaveReq.getExamTestQuestionDeleteReqList()){
            Optional<ExamTestQuestionEntity> examTestQuestionEntity = examTestQuestionRepository.findById(examTestQuestion.getId());
            examTestQuestionEntity.ifPresent(examTestQuestionDeleteList::add);
        }
        examTestQuestionRepository.deleteAll(examTestQuestionDeleteList);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Cập nhật đề thi thành công");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamTestEntity> delete(String id) {
        ResponseData<ExamTestEntity> responseData = new ResponseData<>();
        Optional<ExamTestEntity> examTestExist = examTestRepository.findById(id);
        if(!examTestExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại đề thi");
            return responseData;
        }

        List<ExamEntity> examEntityList = examRepository.findALlByExamTestId(id);
        if(!examEntityList.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Đề thi đã được gán cho bài thi, không được xoá");
            return responseData;
        }

        List<ExamTestQuestionEntity> testQuestionEntityList = examTestQuestionRepository.findAllByExamTestId(id);
        examTestQuestionRepository.deleteAll(testQuestionEntityList);
        examTestRepository.delete(examTestExist.get());
        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Xoá đề thi thành công");
        return responseData;
    }
}
