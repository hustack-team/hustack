package com.hust.baseweb.applications.exam.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.exam.entity.*;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionFilterRes;
import com.hust.baseweb.applications.exam.repository.ExamQuestionRepository;
import com.hust.baseweb.applications.exam.repository.ExamQuestionTagRepository;
import com.hust.baseweb.applications.exam.repository.ExamTagRepository;
import com.hust.baseweb.applications.exam.repository.ExamTestQuestionRepository;
import com.hust.baseweb.applications.exam.service.ExamQuestionService;
import com.hust.baseweb.applications.exam.service.MongoFileService;
import com.hust.baseweb.applications.exam.utils.DataUtils;
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
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamQuestionServiceImpl implements ExamQuestionService {

    ExamQuestionRepository examQuestionRepository;
    ExamTagRepository examTagRepository;
    ExamQuestionTagRepository examQuestionTagRepository;
    ExamTestQuestionRepository examTestQuestionRepository;
    MongoFileService mongoFileService;
    ModelMapper modelMapper;
    EntityManager entityManager;
    ObjectMapper objectMapper;

    @Override
    public Page<ExamQuestionFilterRes> filter(Pageable pageable, ExamQuestionFilterReq examQuestionFilterReq) {
        return examQuestionRepository.filter(
            pageable,
            SecurityUtils.getUserLogin(),
            examQuestionFilterReq.getType(),
            examQuestionFilterReq.getExamTagIds() != null ? List.of(examQuestionFilterReq.getExamTagIds().split(",")) : new ArrayList<>(),
            examQuestionFilterReq.getLevel(),
            examQuestionFilterReq.getExamSubjectId(),
            examQuestionFilterReq.getKeyword()
        );
    }
    private List<String> getIdInListExamTag(List<ExamTagEntity> examTags){
        return examTags.stream()
                       .map(ExamTagEntity::getId)
                       .collect(Collectors.toList());
    }

    @Override
    public ResponseData<ExamQuestionDetailsRes> details(String id) {
        ResponseData<ExamQuestionDetailsRes> responseData = new ResponseData<>();
        if(DataUtils.stringIsNotNullOrEmpty(id)){
            Optional<ExamQuestionDetailsRes> examQuestionEntity = examQuestionRepository.findOneById(id);
            if(examQuestionEntity.isPresent()){
                responseData.setHttpStatus(HttpStatus.OK);
                responseData.setResultCode(HttpStatus.OK.value());
                responseData.setResultMsg("Success");
                responseData.setData(examQuestionEntity.get());
                return responseData;
            }
        }

//        if(DataUtils.stringIsNotNullOrEmpty(code)){
//            Optional<ExamQuestionDetailsRes> examQuestionEntity = examQuestionRepository.findOneByCode(code);
//            if(examQuestionEntity.isPresent()){
//                responseData.setHttpStatus(HttpStatus.OK);
//                responseData.setResultCode(HttpStatus.OK.value());
//                responseData.setResultMsg("Success");
//                responseData.setData(examQuestionEntity.get());
//                return responseData;
//            }
//        }

        responseData.setHttpStatus(HttpStatus.NOT_FOUND);
        responseData.setResultCode(HttpStatus.NOT_FOUND.value());
        responseData.setResultMsg("Chưa tồn tại câu hỏi");
        return responseData;
    }

    @Override
    public ResponseData<ExamQuestionEntity> create(ExamQuestionSaveReq examQuestionSaveReq, MultipartFile[] files) {
        ResponseData<ExamQuestionEntity> responseData = new ResponseData<>();
        Optional<ExamQuestionEntity> examQuestionExist = examQuestionRepository.findByCode(examQuestionSaveReq.getCode());
        if(examQuestionExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.ALREADY_REPORTED);
            responseData.setResultCode(HttpStatus.ALREADY_REPORTED.value());
            responseData.setResultMsg("Đã tồn tại mã câu hỏi");
            return responseData;
        }

        List<String> filePaths = new ArrayList<>();
        if(files != null && files.length > 0){
            filePaths = mongoFileService.storeFiles(files);
        }

        ExamQuestionEntity examQuestionEntity = modelMapper.map(examQuestionSaveReq, ExamQuestionEntity.class);
        examQuestionEntity.setFilePath(String.join(";", filePaths));
        examQuestionEntity = examQuestionRepository.save(examQuestionEntity);

        List<ExamQuestionTagEntity> examQuestionTagEntityList = new ArrayList<>();
        for(ExamTagEntity item: examQuestionSaveReq.getExamTags()){
            ExamQuestionTagEntity examQuestionTagEntity = new ExamQuestionTagEntity();
            examQuestionTagEntity.setId(new ExamQuestionTagKey(item.getId(), examQuestionEntity.getId()));
            examQuestionTagEntityList.add(examQuestionTagEntity);
        }
        examQuestionTagRepository.saveAll(examQuestionTagEntityList);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Thêm mới câu hỏi thành công");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamQuestionEntity> update(ExamQuestionSaveReq examQuestionSaveReq, MultipartFile[] files) {
        ResponseData<ExamQuestionEntity> responseData = new ResponseData<>();
        Optional<ExamQuestionEntity> examQuestionExist = examQuestionRepository.findByCode(examQuestionSaveReq.getCode());
        if(!examQuestionExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại câu hỏi");
            return responseData;
        }

        List<String> filePaths = new ArrayList<>();
        if(files != null && files.length > 0){
            filePaths = mongoFileService.storeFiles(files);
        }

        ExamQuestionEntity examQuestionEntity = modelMapper.map(examQuestionSaveReq, ExamQuestionEntity.class);
        examQuestionEntity.setId(examQuestionExist.get().getId());
        examQuestionEntity.setCreatedBy(examQuestionExist.get().getCreatedBy());
        examQuestionEntity.setCreatedAt(examQuestionExist.get().getCreatedAt());
        examQuestionEntity.setFilePath(
            DataUtils.stringIsNotNullOrEmpty(examQuestionEntity.getFilePath()) ?
                (!filePaths.isEmpty() ?
                    examQuestionEntity.getFilePath() +";"+ String.join(";", filePaths) :
                    examQuestionEntity.getFilePath()) :
                String.join(";", filePaths));
        examQuestionRepository.save(examQuestionEntity);

        for(String filePath: examQuestionSaveReq.getDeletePaths()){
            mongoFileService.deleteByPath(filePath);
        }

        List<ExamTagEntity> examQuestionTagEntityExistList = examTagRepository.findAllByExamQuestionId(examQuestionEntity.getId());
        List<ExamTagEntity> examQuestionTagEntityDeleteList = examQuestionTagEntityExistList.stream()
                                                                                                    .filter(item -> !examQuestionSaveReq.getExamTags().contains(item))
                                                                                                    .collect(Collectors.toList());
        List<ExamTagEntity> examQuestionTagEntityAddList = examQuestionSaveReq.getExamTags().stream()
                                                                                         .filter(item -> !examQuestionTagEntityExistList.contains(item))
                                                                                         .collect(Collectors.toList());

        for(ExamTagEntity item: examQuestionTagEntityDeleteList){
            ExamQuestionTagEntity examQuestionTagEntity = new ExamQuestionTagEntity();
            examQuestionTagEntity.setId(new ExamQuestionTagKey(item.getId(), examQuestionEntity.getId()));
            examQuestionTagRepository.delete(examQuestionTagEntity);
        }
        for(ExamTagEntity item: examQuestionTagEntityAddList){
            ExamQuestionTagEntity examQuestionTagEntity = new ExamQuestionTagEntity();
            examQuestionTagEntity.setId(new ExamQuestionTagKey(item.getId(), examQuestionEntity.getId()));
            examQuestionTagRepository.save(examQuestionTagEntity);
        }

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Cập nhật câu hỏi thành công");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamQuestionEntity> delete(String id) {
        ResponseData<ExamQuestionEntity> responseData = new ResponseData<>();
        Optional<ExamQuestionEntity> examQuestionExist = examQuestionRepository.findById(id);
        if(!examQuestionExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại câu hỏi");
            return responseData;
        }

        List<ExamTestQuestionEntity> examTestQuestionEntityList = examTestQuestionRepository.findAllByExamQuestionId(id);
        if(!examTestQuestionEntityList.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Câu hỏi đã được gán cho đề thi, không được xoá");
            return responseData;
        }

        if(examQuestionExist.get().getFilePath() != null){
            String[] filePaths = examQuestionExist.get().getFilePath().split(";");
            for(String filePath: filePaths){
                if(DataUtils.stringIsNotNullOrEmpty(filePath)){
                    mongoFileService.deleteByPath(filePath);
                }
            }
        }

        List<ExamQuestionTagEntity> examQuestionTagEntityList = examQuestionTagRepository.findALLById_ExamQuestionId(id);
        if(!examQuestionTagEntityList.isEmpty()){
            examQuestionTagRepository.deleteAll(examQuestionTagEntityList);
        }

        examQuestionRepository.delete(examQuestionExist.get());
        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Xoá câu hỏi thành công");
        return responseData;
    }
}
