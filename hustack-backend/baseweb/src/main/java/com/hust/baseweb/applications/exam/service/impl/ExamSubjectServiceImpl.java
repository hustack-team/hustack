package com.hust.baseweb.applications.exam.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.exam.entity.ExamQuestionEntity;
import com.hust.baseweb.applications.exam.entity.ExamSubjectEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamSubjectDeleteReq;
import com.hust.baseweb.applications.exam.model.request.ExamSubjectFilterReq;
import com.hust.baseweb.applications.exam.model.request.ExamSubjectSaveReq;
import com.hust.baseweb.applications.exam.repository.ExamQuestionRepository;
import com.hust.baseweb.applications.exam.repository.ExamSubjectRepository;
import com.hust.baseweb.applications.exam.service.ExamSubjectService;
import com.hust.baseweb.applications.exam.utils.Constants;
import com.hust.baseweb.applications.exam.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamSubjectServiceImpl implements ExamSubjectService {

    ExamSubjectRepository examSubjectRepository;
    ExamQuestionRepository examQuestionRepository;
    EntityManager entityManager;
    ModelMapper modelMapper;
    ObjectMapper objectMapper;

    @Override
    public Page<ExamSubjectEntity> filter(Pageable pageable, ExamSubjectFilterReq examSubjectFilterReq) {
        return examSubjectRepository.filter(
            pageable,
            SecurityUtils.getUserLogin(),
            examSubjectFilterReq.getStatus() != null ? examSubjectFilterReq.getStatus().name() : null,
            examSubjectFilterReq.getKeyword()
        );
    }

    @Override
    public List<ExamSubjectEntity> getAll() {
        return examSubjectRepository.findAllByStatusOrderByName(Constants.Status.ACTIVE);
    }

    @Override
    public ResponseData<ExamSubjectEntity> create(ExamSubjectSaveReq examSubjectSaveReq) {
        ResponseData<ExamSubjectEntity> responseData = new ResponseData<>();
        Optional<ExamSubjectEntity> examSubjectExist = examSubjectRepository.findByCode(examSubjectSaveReq.getCode());
        if(examSubjectExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.ALREADY_REPORTED);
            responseData.setResultCode(HttpStatus.ALREADY_REPORTED.value());
            responseData.setResultMsg("Đã tồn tại mã môn học");
            return responseData;
        }

        ExamSubjectEntity examSubjectEntity = modelMapper.map(examSubjectSaveReq, ExamSubjectEntity.class);
        examSubjectRepository.save(examSubjectEntity);
        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Thêm mới môn học thành công");
        return responseData;
    }

    @Override
    public ResponseData<ExamSubjectEntity> update(ExamSubjectSaveReq examSubjectSaveReq) {
        ResponseData<ExamSubjectEntity> responseData = new ResponseData<>();
        Optional<ExamSubjectEntity> examSubjectExist = examSubjectRepository.findByCode(examSubjectSaveReq.getCode());
        if(!examSubjectExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại môn học");
            return responseData;
        }

        ExamSubjectEntity examSubjectEntity = modelMapper.map(examSubjectSaveReq, ExamSubjectEntity.class);
        examSubjectEntity.setId(examSubjectExist.get().getId());
        examSubjectEntity.setCreatedBy(examSubjectExist.get().getCreatedBy());
        examSubjectEntity.setCreatedAt(examSubjectExist.get().getCreatedAt());
        examSubjectRepository.save(examSubjectEntity);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Cập nhật môn học thành công");
        return responseData;
    }

    @Override
    public ResponseData<ExamSubjectEntity> delete(ExamSubjectDeleteReq examSubjectDeleteReq) {
        ResponseData<ExamSubjectEntity> responseData = new ResponseData<>();
        Optional<ExamSubjectEntity> examSubjectExist = examSubjectRepository.findById(examSubjectDeleteReq.getId());
        if(!examSubjectExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại môn học");
            return responseData;
        }

        List<ExamQuestionEntity> examQuestionEntityList = examQuestionRepository.findAllByExamSubjectId(examSubjectDeleteReq.getId());
        if(!examQuestionEntityList.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Đã có câu hỏi thuộc môn học, không được xoá");
            return responseData;
        }

        examSubjectRepository.delete(examSubjectExist.get());
        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Xoá môn học thành công");
        return responseData;
    }
}
