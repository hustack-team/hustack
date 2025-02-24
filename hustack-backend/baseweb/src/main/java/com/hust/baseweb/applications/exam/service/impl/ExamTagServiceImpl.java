package com.hust.baseweb.applications.exam.service.impl;

import com.hust.baseweb.applications.exam.entity.*;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.repository.*;
import com.hust.baseweb.applications.exam.service.ExamTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ExamTagServiceImpl implements ExamTagService {

    private final ExamTagRepository examTagRepository;
    private final ModelMapper modelMapper;


    @Override
    public List<ExamTagEntity> getAll() {
        return examTagRepository.findAllByOrderByNameAsc();
    }

    @Override
    public ResponseData<ExamTagEntity> create(ExamTagSaveReq examTagSaveReq) {
        ResponseData<ExamTagEntity> responseData = new ResponseData<>();

        Optional<ExamTagEntity> examTagEntityExist = examTagRepository.findByName(examTagSaveReq.getName());
        if(examTagEntityExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.ALREADY_REPORTED);
            responseData.setResultCode(HttpStatus.ALREADY_REPORTED.value());
            responseData.setResultMsg("Đã tồn tại tag câu hỏi");
            return responseData;
        }

        ExamTagEntity examTagEntity = modelMapper.map(examTagSaveReq, ExamTagEntity.class);
        examTagRepository.save(examTagEntity);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Thêm mới tag câu hỏi thành công");
        return responseData;
    }

    @Override
    public ResponseData<ExamTagEntity> update(ExamTagSaveReq examTagSaveReq) {
        ResponseData<ExamTagEntity> responseData = new ResponseData<>();

        Optional<ExamTagEntity> examTagEntityExist = examTagRepository.findById(examTagSaveReq.getId());
        if(!examTagEntityExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại tag câu hỏi");
            return responseData;
        }

        ExamTagEntity examTagEntity = modelMapper.map(examTagSaveReq, ExamTagEntity.class);
        examTagEntity.setId(examTagEntityExist.get().getId());
        examTagEntity.setCreatedBy(examTagEntityExist.get().getCreatedBy());
        examTagEntity.setCreatedAt(examTagEntityExist.get().getCreatedAt());
        examTagRepository.save(examTagEntity);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Cập nhật tag câu hỏi thành công");
        return responseData;
    }
}
