package com.hust.baseweb.applications.exam.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.exam.entity.*;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionFilterRes;
import com.hust.baseweb.applications.exam.model.response.MyExamDetailsRes;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        StringBuilder sql = new StringBuilder();
        sql.append("select\n" +
                   "    eq.*,\n" +
                   "    es.name as examSubjectName\n" +
                   "from\n" +
                   "    exam_question eq\n" +
                   "left join exam_subject es on\n" +
                   "    es.id = eq.exam_subject_id\n" +
                   "left join exam_question_tag eqt on\n" +
                   "    eq.id = eqt.exam_question_id\n" +
                   "left join exam_tag et on\n" +
                   "    et.id = eqt.exam_tag_id\n" +
                   "where\n" +
                   "    eq.created_by = :userLogin \n"+
                   "    and es.status = 'ACTIVE' \n");
        if(examQuestionFilterReq.getType() != null){
            sql.append("and\n" +
                       "    eq.type = :type \n");
        }
        if(!examQuestionFilterReq.getExamTags().isEmpty()){
            sql.append("and\n" +
                       "    eqt.exam_tag_id in (:examTagIds) \n");
        }
        if(examQuestionFilterReq.getLevel() != null){
            sql.append("and\n" +
                       "    eq.level = :level \n");
        }
        if(examQuestionFilterReq.getExamSubjectId() != null){
            sql.append("and\n" +
                       "    eq.exam_subject_id = :examSubjectId \n");
        }
        if(DataUtils.stringIsNotNullOrEmpty(examQuestionFilterReq.getKeyword())){
            sql.append("and\n" +
                       "    ((lower(eq.code) like CONCAT('%', LOWER(:keyword),'%')) or \n" +
                       "    (lower(eq.content) like CONCAT('%', LOWER(:keyword),'%'))) \n");
        }
        sql.append("group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, \n" +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3, \n" +
                   "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer, \n" +
                   "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by,\n" +
                   "    eq.exam_subject_id, eq.level ,es.name\n");
        sql.append("order by created_at desc\n");

        Query query = entityManager.createNativeQuery(sql.toString());
        Query count = entityManager.createNativeQuery("select count(1) FROM (" + sql + ") as count");
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());

        query.setParameter("userLogin", SecurityUtils.getUserLogin());
        count.setParameter("userLogin", SecurityUtils.getUserLogin());
        if(examQuestionFilterReq.getType() != null){
            query.setParameter("type", examQuestionFilterReq.getType());
            count.setParameter("type", examQuestionFilterReq.getType());
        }
        if(!examQuestionFilterReq.getExamTags().isEmpty()){
            query.setParameter("examTagIds", getIdInListExamTag(examQuestionFilterReq.getExamTags()));
            count.setParameter("examTagIds", getIdInListExamTag(examQuestionFilterReq.getExamTags()));
        }
        if(examQuestionFilterReq.getLevel() != null){
            query.setParameter("level", examQuestionFilterReq.getLevel());
            count.setParameter("level", examQuestionFilterReq.getLevel());
        }
        if(examQuestionFilterReq.getExamSubjectId() != null){
            query.setParameter("examSubjectId", examQuestionFilterReq.getExamSubjectId());
            count.setParameter("examSubjectId", examQuestionFilterReq.getExamSubjectId());
        }
        if(DataUtils.stringIsNotNullOrEmpty(examQuestionFilterReq.getKeyword())){
            query.setParameter("keyword", DataUtils.escapeSpecialCharacters(examQuestionFilterReq.getKeyword()));
            count.setParameter("keyword", DataUtils.escapeSpecialCharacters(examQuestionFilterReq.getKeyword()));
        }

        long totalRecord = ((Number) count.getSingleResult()).longValue();
        List<Object[]> result = query.getResultList();
        List<ExamQuestionFilterRes> list = new ArrayList<>();
        if (!Objects.isNull(result) && !result.isEmpty()) {
            for (Object[] obj : result) {
                ExamQuestionFilterRes item = new ExamQuestionFilterRes();
                item.setId(DataUtils.safeToString(obj[0]));
                item.setCode(DataUtils.safeToString(obj[1]));
                item.setType(DataUtils.safeToInt(obj[2]));
                item.setContent(DataUtils.safeToString(obj[3]));
                item.setFilePath(DataUtils.safeToString(obj[4]));
                item.setNumberAnswer(DataUtils.safeToInt(obj[5]));
                item.setContentAnswer1(DataUtils.safeToString(obj[6]));
                item.setContentAnswer2(DataUtils.safeToString(obj[7]));
                item.setContentAnswer3(DataUtils.safeToString(obj[8]));
                item.setContentAnswer4(DataUtils.safeToString(obj[9]));
                item.setContentAnswer5(DataUtils.safeToString(obj[10]));
                item.setMultichoice(DataUtils.safeToBoolean(obj[11]));
                item.setAnswer(DataUtils.safeToString(obj[12]));
                item.setExplain(DataUtils.safeToString(obj[13]));
//                item.setCreatedAt(DataUtils.safeTO(obj[14]));
//                item.setUpdatedAt(DataUtils.safeToString(obj[15]));
                item.setCreatedBy(DataUtils.safeToString(obj[16]));
                item.setUpdatedBy(DataUtils.safeToString(obj[17]));
                item.setExamSubjectId(DataUtils.safeToString(obj[18]));
                item.setLevel(DataUtils.safeToString(obj[19]));
                item.setExamSubjectName(DataUtils.safeToString(obj[20]));
                item.setExamTags(examTagRepository.findAllByExamQuestionId(item.getId()));

                list.add(item);
            }
        }
        return new PageImpl<>(list, pageable, totalRecord);
    }
    private List<String> getIdInListExamTag(List<ExamTagEntity> examTags){
        return examTags.stream()
                       .map(ExamTagEntity::getId)
                       .collect(Collectors.toList());
    }

    @Override
    public ResponseData<ExamQuestionDetailsRes> details(ExamQuestionDetailsReq examQuestionDetailsReq) {
        ResponseData<ExamQuestionDetailsRes> responseData = new ResponseData<>();
        if(DataUtils.stringIsNotNullOrEmpty(examQuestionDetailsReq.getId())){
            Optional<ExamQuestionDetailsRes> examQuestionEntity = examQuestionRepository.findOneById(examQuestionDetailsReq.getId());
            if(examQuestionEntity.isPresent()){
                responseData.setHttpStatus(HttpStatus.OK);
                responseData.setResultCode(HttpStatus.OK.value());
                responseData.setResultMsg("Success");
                responseData.setData(examQuestionEntity.get());
                return responseData;
            }
        }

        if(DataUtils.stringIsNotNullOrEmpty(examQuestionDetailsReq.getCode())){
            Optional<ExamQuestionDetailsRes> examQuestionEntity = examQuestionRepository.findOneByCode(examQuestionDetailsReq.getCode());
            if(examQuestionEntity.isPresent()){
                responseData.setHttpStatus(HttpStatus.OK);
                responseData.setResultCode(HttpStatus.OK.value());
                responseData.setResultMsg("Success");
                responseData.setData(examQuestionEntity.get());
                return responseData;
            }
        }

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

        List<String> filePaths = mongoFileService.storeFiles(files);

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
        if(files.length > 0){
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
    public ResponseData<ExamQuestionEntity> delete(ExamQuestionDeleteReq examQuestionDeleteReq) {
        ResponseData<ExamQuestionEntity> responseData = new ResponseData<>();
        Optional<ExamQuestionEntity> examQuestionExist = examQuestionRepository.findById(examQuestionDeleteReq.getId());
        if(!examQuestionExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại câu hỏi");
            return responseData;
        }

        List<ExamTestQuestionEntity> examTestQuestionEntityList = examTestQuestionRepository.findAllByExamQuestionId(examQuestionDeleteReq.getId());
        if(!examTestQuestionEntityList.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Câu hỏi đã được gán cho đề thi, không được xoá");
            return responseData;
        }

        String[] filePaths = examQuestionExist.get().getFilePath().split(";");
        for(String filePath: filePaths){
            if(DataUtils.stringIsNotNullOrEmpty(filePath)){
                mongoFileService.deleteByPath(filePath);
            }
        }

        examQuestionRepository.delete(examQuestionExist.get());
        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Xoá câu hỏi thành công");
        return responseData;
    }
}
