package com.hust.baseweb.applications.exam.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.exam.entity.*;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionFilterRes;
import com.hust.baseweb.applications.exam.repository.*;
import com.hust.baseweb.applications.exam.service.ExamQuestionService;
import com.hust.baseweb.applications.exam.service.MongoFileService;
import com.hust.baseweb.applications.exam.utils.DataUtils;
import com.hust.baseweb.applications.exam.utils.SecurityUtils;
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
    ExamQuestionAnswerRepository examQuestionAnswerRepository;
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
        if(StringUtils.isNotEmpty(id)){
            Optional<ExamQuestionDetailsRes> examQuestionEntity = examQuestionRepository.findOneById(id);
            if(examQuestionEntity.isPresent()){
                responseData.setHttpStatus(HttpStatus.OK);
                responseData.setResultCode(HttpStatus.OK.value());
                responseData.setResultMsg("Success");
                responseData.setData(examQuestionEntity.get());
                return responseData;
            }
        }

//        if(StringUtils.isNotEmpty(code)){
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
        List<String> remainingFilePaths = new ArrayList<>();
        for (String filePath : filePaths) {
            if (filePath != null) {
                boolean exist = false;
                for(ExamQuestionAnswerSaveReq answerSaveReq: examQuestionSaveReq.getAnswers()){
                    if (filePath.contains("_answer_"+answerSaveReq.getOrder())) {
                        answerSaveReq.setFile(filePath);
                        exist = true;
                    }
                }
                if(!exist){
                    remainingFilePaths.add(filePath);
                }
            }
        }
        examQuestionEntity.setFilePath(String.join(";", remainingFilePaths));
        examQuestionEntity = examQuestionRepository.save(examQuestionEntity);

        List<ExamQuestionTagEntity> examQuestionTagEntityList = new ArrayList<>();
        for(ExamTagEntity item: examQuestionSaveReq.getExamTags()){
            ExamQuestionTagEntity examQuestionTagEntity = new ExamQuestionTagEntity();
            examQuestionTagEntity.setId(new ExamQuestionTagKey(item.getId(), examQuestionEntity.getId()));
            examQuestionTagEntityList.add(examQuestionTagEntity);
        }
        examQuestionTagRepository.saveAll(examQuestionTagEntityList);

        List<ExamQuestionAnswerEntity> examQuestionAnswerEntities = new ArrayList<>();
        for(ExamQuestionAnswerSaveReq answerSaveReq: examQuestionSaveReq.getAnswers()){
            examQuestionAnswerEntities.add(
                ExamQuestionAnswerEntity
                    .builder()
                    .examQuestionId(examQuestionEntity.getId())
                    .order(answerSaveReq.getOrder())
                    .content(answerSaveReq.getContent())
                    .file(answerSaveReq.getFile())
                    .build());
        }
        examQuestionAnswerRepository.saveAll(examQuestionAnswerEntities);

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
        List<String> remainingFilePaths = new ArrayList<>();
        for (String filePath : filePaths) {
            if (filePath != null) {
                boolean exist = false;
                for(ExamQuestionAnswerSaveReq answerSaveReq: examQuestionSaveReq.getAnswers()){
                    if (filePath.contains("_answer_"+answerSaveReq.getOrder())) {
                        answerSaveReq.setFile(filePath);
                        exist = true;
                    }
                }
                if(!exist){
                    remainingFilePaths.add(filePath);
                }
            }
        }
        examQuestionEntity.setFilePath(
            StringUtils.isNotEmpty(examQuestionEntity.getFilePath()) ?
                (!remainingFilePaths.isEmpty() ?
                    examQuestionEntity.getFilePath() +";"+ String.join(";", remainingFilePaths) :
                    examQuestionEntity.getFilePath()) :
                String.join(";", remainingFilePaths));
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

        List<ExamQuestionAnswerEntity> examQuestionAnswerEntities = new ArrayList<>();
        for(ExamQuestionAnswerSaveReq answerSaveReq: examQuestionSaveReq.getAnswers()){
            examQuestionAnswerEntities.add(
                ExamQuestionAnswerEntity
                    .builder()
                    .id(answerSaveReq.getId())
                    .examQuestionId(examQuestionEntity.getId())
                    .order(answerSaveReq.getOrder())
                    .content(answerSaveReq.getContent())
                    .file(answerSaveReq.getFile())
                    .build());
        }
        examQuestionAnswerRepository.saveAll(examQuestionAnswerEntities);

        for(ExamQuestionAnswerSaveReq answerSaveReq: examQuestionSaveReq.getAnswersDelete()){
            examQuestionAnswerRepository.deleteById(answerSaveReq.getId());
            if(StringUtils.isNotEmpty(answerSaveReq.getFile())){
                mongoFileService.deleteByPath(answerSaveReq.getFile());
            }
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

        List<ExamQuestionAnswerEntity> questionAnswerEntityList = examQuestionAnswerRepository.findAllByExamQuestionId(id);
        if(!questionAnswerEntityList.isEmpty()){
            List<String> listFileDelete = questionAnswerEntityList.stream()
                                                                  .map(ExamQuestionAnswerEntity::getFile)
                                                                  .collect(Collectors.toList());
            for(String filePath: listFileDelete){
                if(StringUtils.isNotEmpty(filePath)){
                    mongoFileService.deleteByPath(filePath);
                }
            }
            examQuestionAnswerRepository.deleteAll(questionAnswerEntityList);
        }

        if(examQuestionExist.get().getFilePath() != null){
            String[] filePaths = examQuestionExist.get().getFilePath().split(";");
            for(String filePath: filePaths){
                if(StringUtils.isNotEmpty(filePath)){
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
