package com.hust.baseweb.applications.exam.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hust.baseweb.applications.exam.entity.*;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.*;
import com.hust.baseweb.applications.exam.repository.*;
import com.hust.baseweb.applications.exam.service.ExamService;
import com.hust.baseweb.applications.exam.service.ExamTestService;
import com.hust.baseweb.applications.exam.service.MongoFileService;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamServiceImpl implements ExamService {

    ExamRepository examRepository;
    ExamTestRepository examTestRepository;
    ExamStudentRepository examStudentRepository;
    ExamResultRepository examResultRepository;
    ExamResultDetailsRepository examResultDetailsRepository;
    ExamExamTestRepository examExamTestRepository;
    ExamStudentTestRepository examStudentTestRepository;
    EntityManager entityManager;
    ModelMapper modelMapper;
    ObjectMapper objectMapper;
    ExamTestService examTestService;
    MongoFileService mongoFileService;


    @Override
    public Page<ExamEntity> filter(Pageable pageable, ExamFilterReq examFilterReq) {
        return examRepository.filter(
            pageable,
            SecurityUtils.getUserLogin(),
            examFilterReq.getStatus(),
            DataUtils.formatStringValueSqlToLocalDateTime(examFilterReq.getStartTimeFrom(), true),
            DataUtils.formatStringValueSqlToLocalDateTime(examFilterReq.getStartTimeTo(), false),
            DataUtils.formatStringValueSqlToLocalDateTime(examFilterReq.getEndTimeFrom(), true),
            DataUtils.formatStringValueSqlToLocalDateTime(examFilterReq.getEndTimeTo(), false),
            examFilterReq.getKeyword()
        );
    }

    @Override
    public ResponseData<ExamDetailsRes> details(String id) {
        ResponseData<ExamDetailsRes> responseData = new ResponseData<>();

        Optional<ExamDetailsRes> exam = examRepository.detailExamById(id);
        if(!exam.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn kỳ thi");
            return responseData;
        }

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Success");
        responseData.setData(exam.get());
        return responseData;
    }

    @Override
    public ResponseData<List<ExamStudentResultDetailsRes>> detailStudentExam(String examExamTestId) {
        ResponseData<List<ExamStudentResultDetailsRes>> responseData = new ResponseData<>();

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Success");
        responseData.setData(examStudentRepository.findAllWithResult(examExamTestId));
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamEntity> create(ExamSaveReq examSaveReq) {
        ResponseData<ExamEntity> responseData = new ResponseData<>();

        List<ExamTestEntity> examTestExist = examTestRepository.findAllByExamTestIds(examSaveReq.getExamExamTests()
                                                                                                .stream()
                                                                                                .map(ExamExamTestEntity::getExamTestId)
                                                                                                .collect(Collectors.toList()));
        if(examTestExist.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại đề thi");
            return responseData;
        }

        Optional<ExamEntity> examEntityExist = examRepository.findByCode(examSaveReq.getCode());
        if(examEntityExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Đã tồn tại kỳ thi");
            return responseData;
        }

        ExamEntity examEntity = modelMapper.map(examSaveReq, ExamEntity.class);
        examEntity.setStartTime(DataUtils.formatStringToLocalDateTime(examSaveReq.getStartTime()));
        examEntity.setEndTime(DataUtils.formatStringToLocalDateTime(examSaveReq.getEndTime()));
        examEntity = examRepository.save(examEntity);

        List<ExamExamTestEntity> examExamTests = new ArrayList<>();
        for(ExamExamTestEntity examExamTest: examSaveReq.getExamExamTests()){
            examExamTests.add(ExamExamTestEntity
                                  .builder()
                                  .examId(examEntity.getId())
                                  .examTestId(examExamTest.getExamTestId())
                                  .build());
        }
        examExamTests = examExamTestRepository.saveAll(examExamTests);

        if(!examSaveReq.getExamStudents().isEmpty()){
            List<ExamStudentTestEntity> examStudentTestEntities = new ArrayList<>();
            for(ExamStudentSaveReq examStudent: examSaveReq.getExamStudents()){
                ExamStudentEntity examStudentEntity = modelMapper.map(examStudent, ExamStudentEntity.class);
                examStudentEntity = examStudentRepository.save(examStudentEntity);
                for(ExamExamTestEntity examExamTest: examExamTests){
                    examStudentTestEntities.add(ExamStudentTestEntity
                                                    .builder()
                                                    .examExamTestId(examExamTest.getId())
                                                    .examStudentId(examStudentEntity.getId())
                                                    .build());
                }
            }
            examStudentTestRepository.saveAll(examStudentTestEntities);
        }

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Thêm mới kỳ thi thành công");
        return responseData;
    }

    @Override
    public ResponseData<ExamPreviewUpdateRes> previewUpdate(String id) {
        ResponseData<ExamPreviewUpdateRes> responseData = new ResponseData<>();

        Optional<ExamDetailsRes> examExist = examRepository.detailExamById(id);
        if(!examExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn kỳ thi");
            return responseData;
        }
        ExamPreviewUpdateRes examPreviewUpdateRes = modelMapper.map(examExist.get(), ExamPreviewUpdateRes.class);
        examPreviewUpdateRes.setExamExamTests(examExamTestRepository.findPreviewUpdateByExamId(id));
        examPreviewUpdateRes.setExamStudents(examStudentRepository.findALlByExamId(id));

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setData(examPreviewUpdateRes);
        responseData.setResultMsg("Success");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamEntity> update(ExamSaveReq examSaveReq) {
        ResponseData<ExamEntity> responseData = new ResponseData<>();

        List<ExamTestEntity> examTestExist = examTestRepository.findAllByExamTestIds(examSaveReq.getExamExamTests()
                                                                                                .stream()
                                                                                                .map(ExamExamTestEntity::getExamTestId)
                                                                                                .collect(Collectors.toList()));
        if(examTestExist.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại đề thi");
            return responseData;
        }

        Optional<ExamEntity> examEntityExist = examRepository.findByCode(examSaveReq.getCode());
        if(!examEntityExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại kỳ thi");
            return responseData;
        }

        ExamEntity examEntity = modelMapper.map(examSaveReq, ExamEntity.class);
        examEntity.setId(examEntityExist.get().getId());
        examEntity.setCreatedBy(examEntityExist.get().getCreatedBy());
        examEntity.setCreatedAt(examEntityExist.get().getCreatedAt());
        examEntity.setStartTime(DataUtils.formatStringToLocalDateTime(examSaveReq.getStartTime()));
        examEntity.setEndTime(DataUtils.formatStringToLocalDateTime(examSaveReq.getEndTime()));
        examRepository.save(examEntity);

        if(!examSaveReq.getExamStudentDeletes().isEmpty()){
            List<String> examStudentIds = new ArrayList<>();
            for(ExamStudentSaveReq examStudent: examSaveReq.getExamStudentDeletes()){
                examStudentIds.add(examStudent.getId());

                List<ExamStudentTestEntity> examStudentTestEntities = examStudentTestRepository.findAllByExamStudentId(examStudent.getId());
                examStudentTestRepository.deleteAll(examStudentTestEntities);
            }
            examStudentRepository.deleteAllById(examStudentIds);
        }

        List<ExamExamTestEntity> examExamTests = new ArrayList<>();
        for(ExamExamTestEntity examExamTest: examSaveReq.getExamExamTests()){
            if(!StringUtils.isNotEmpty(examExamTest.getId())){
                examExamTests.add(ExamExamTestEntity
                                      .builder()
                                      .examId(examEntity.getId())
                                      .examTestId(examExamTest.getExamTestId())
                                      .build());
            }
        }
        examExamTests = examExamTestRepository.saveAll(examExamTests);
        if(!examSaveReq.getExamExamTestDeletes().isEmpty()){
            List<ExamStudentUpdateDeleteRes> examStudentUpdateDeleteRes = examStudentRepository
                .findAllWithExamStudentTestByExamExamTestIds(examSaveReq.getExamExamTestDeletes()
                                                                        .stream()
                                                                        .map(ExamExamTestEntity::getId)
                                                                        .collect(Collectors.toList()));

            List<ExamStudentTestEntity> examStudentTestEntities = examStudentUpdateDeleteRes.stream()
                                                                                            .map(ExamStudentUpdateDeleteRes::getExamStudentTests)
                                                                                            .flatMap(json -> {
                                                                                                try {
                                                                                                    return objectMapper.readValue(json, new TypeReference<List<ExamStudentTestEntity>>() {})
                                                                                                                       .stream();
                                                                                                } catch (Exception e) {
                                                                                                    log.error(e.getMessage(), e);
                                                                                                    return Stream.empty();
                                                                                                }
                                                                                            })
                                                                                            .collect(Collectors.toList());
            examStudentTestRepository.deleteAll(examStudentTestEntities);

            examExamTestRepository.deleteAll(examSaveReq.getExamExamTestDeletes());
        }

        List<ExamStudentTestEntity> examStudentTestEntities = new ArrayList<>();
        for(ExamStudentSaveReq examStudent: examSaveReq.getExamStudents()){
            if(StringUtils.isNotEmpty(examStudent.getId())){
                for(ExamExamTestEntity examExamTest: examExamTests){
                    examStudentTestEntities.add(ExamStudentTestEntity
                                                    .builder()
                                                    .examExamTestId(examExamTest.getId())
                                                    .examStudentId(examStudent.getId())
                                                    .build());
                }
            }
        }
        List<ExamExamTestEntity> examExamTestEntities = new ArrayList<>(examSaveReq.getExamExamTests()
                                                                                   .stream()
                                                                                   .filter(item -> StringUtils.isNotEmpty(item.getId()))
                                                                                   .collect(Collectors.toList()));
        examExamTestEntities.addAll(examExamTests);
        if(!examSaveReq.getExamStudents().isEmpty()){
            for(ExamStudentSaveReq examStudent: examSaveReq.getExamStudents()){
                if(!StringUtils.isNotEmpty(examStudent.getId())){
                    ExamStudentEntity examStudentEntity = modelMapper.map(examStudent, ExamStudentEntity.class);
                    examStudentEntity = examStudentRepository.save(examStudentEntity);

                    for(ExamExamTestEntity examExamTest: examExamTestEntities){
                        examStudentTestEntities.add(ExamStudentTestEntity
                                                        .builder()
                                                        .examExamTestId(examExamTest.getId())
                                                        .examStudentId(examStudentEntity.getId())
                                                        .build());
                    }
                }
            }
        }
        examStudentTestRepository.saveAll(examStudentTestEntities);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Cập nhật kỳ thi thành công");
        return responseData;
    }

    @Override
    public ResponseData<ExamEntity> delete(String id) {
        ResponseData<ExamEntity> responseData = new ResponseData<>();

        Optional<ExamEntity> examEntityExist = examRepository.findById(id);
        if(!examEntityExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại kỳ thi");
            return responseData;
        }

        List<ExamResultEntity> examResultEntities = examResultRepository.findAllByExamId(id);
        if(!examResultEntities.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Đã có học sinh hoàn thành bài thi, không được xoá");
            return responseData;
        }

        List<ExamExamTestEntity> examExamTestEntities = examExamTestRepository.findAllByExamId(id);
        for(ExamExamTestEntity examExamTest: examExamTestEntities){
            List<ExamStudentTestEntity> examStudentTestEntities = examStudentTestRepository.findAllByExamExamTestId(examExamTest.getId());
            List<ExamStudentEntity> examStudentEntities = examStudentRepository.findAllById(examStudentTestEntities
                                                                                                .stream()
                                                                                                .map(ExamStudentTestEntity::getExamStudentId)
                                                                                                .collect(Collectors.toList()));
            examStudentTestRepository.deleteAll(examStudentTestEntities);
            examStudentRepository.deleteAll(examStudentEntities);
        }
        examExamTestRepository.deleteAll(examExamTestEntities);

        examRepository.delete(examEntityExist.get());

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Xoá kỳ thi thành công");
        return responseData;
    }

    @Override
    public Page<MyExamFilterRes> filterMyExam(Pageable pageable, MyExamFilterReq myExamFilterReq) {
        return examRepository.filterMyExam(
            pageable,
            SecurityUtils.getUserLogin(),
            myExamFilterReq.getStatus(),
            myExamFilterReq.getKeyword()
        );
    }

    @Override
    public ResponseData<List<MyExamTestWithResultRes>> getListTestMyExam(String examId) {
        ResponseData<List<MyExamTestWithResultRes>> responseData = new ResponseData<>();

        List<MyExamTestWithResultRes> list = examTestRepository.findAllWithResultByExamId(SecurityUtils.getUserLogin(), examId);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setData(list);
        responseData.setResultMsg("Success");
        return responseData;
    }

    @Override
    public ResponseData<MyExamDetailsRes> detailsMyExam(String examStudentTestId) {
        ResponseData<MyExamDetailsRes> responseData = new ResponseData<>();

        Optional<MyExamDetailsResDB> myExamDetailsResDB = examRepository.detailsMyExam(
            SecurityUtils.getUserLogin(),
            examStudentTestId
        );
        if(!myExamDetailsResDB.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại bài thi");
            return responseData;
        }
        MyExamDetailsRes myExamDetailsRes = new MyExamDetailsRes(myExamDetailsResDB.get(), null, modelMapper);

        if(!StringUtils.isNotEmpty(myExamDetailsRes.getExamResultId())){
            LocalDateTime now = DataUtils.getTimeNowWithZone();
            if(now.isBefore(DataUtils.formatStringToLocalDateTimeFull(myExamDetailsRes.getStartTime()))){
                responseData.setHttpStatus(HttpStatus.NOT_FOUND);
                responseData.setResultCode(HttpStatus.NOT_FOUND.value());
                responseData.setData(myExamDetailsRes);
                responseData.setResultMsg("Chưa đến thời gian thi");
                return responseData;
            }
            if(now.isAfter(DataUtils.formatStringToLocalDateTimeFull(myExamDetailsRes.getEndTime()))){
                responseData.setHttpStatus(HttpStatus.NOT_FOUND);
                responseData.setResultCode(HttpStatus.NOT_FOUND.value());
                responseData.setData(myExamDetailsRes);
                responseData.setResultMsg("Đã hết thời gian thi");
                return responseData;
            }
        }

        myExamDetailsRes.setQuestionList(examTestRepository.getMyExamQuestionDetails(examStudentTestId));

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setData(myExamDetailsRes);
        responseData.setResultMsg("Success");
        return responseData;
    }

    @Override
    public ResponseData<ExamResultEntity> startDoingMyExam(String examStudentTestId) {
        ResponseData<ExamResultEntity> responseData = new ResponseData<>();
        ExamResultEntity examResultEntity = new ExamResultEntity();

        Optional<ExamResultEntity> examResultExist = examResultRepository.findByExamStudentTestId(examStudentTestId);
        if(examResultExist.isPresent()){
            if(Boolean.FALSE.equals(examResultExist.get().getSubmitAgain())){
                responseData.setHttpStatus(HttpStatus.NOT_FOUND);
                responseData.setResultCode(HttpStatus.NOT_FOUND.value());
                responseData.setResultMsg("Thí sinh đã làm bài thi hoặc đã vào bài thi. Cần liên hệ Giảng viên coi thi để tiếp tục làm bài!");
                return responseData;
            }else{
                examResultEntity = examResultExist.get();
            }
        }else{
            examResultEntity.setExamStudentTestId(examStudentTestId);
            examResultEntity.setSubmitAgain(false);
            examResultEntity = examResultRepository.save(examResultEntity);
        }

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setData(examResultEntity);
        responseData.setResultMsg("Success");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamResultEntity> doingMyExam(MyExamResultSaveReq myExamResultSaveReq, MultipartFile[] files) {
        ResponseData<ExamResultEntity> responseData = new ResponseData<>();

        Optional<ExamResultEntity> examResultExist = examResultRepository.findById(myExamResultSaveReq.getId());
        if(examResultExist.isEmpty()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Not Found!");
            return responseData;
        }
        ExamResultEntity examResultEntity = modelMapper.map(examResultExist.get(), ExamResultEntity.class);
        examResultEntity.setSubmitedAt(LocalDateTime.now());
        examResultEntity.setTotalTime(myExamResultSaveReq.getTotalTime());
        examResultEntity = examResultRepository.save(examResultEntity);

        if(files != null){
            for(MultipartFile file: files){
                String filename = file.getOriginalFilename();
                for(MyExamResultDetailsSaveReq examResultDetails: myExamResultSaveReq.getExamResultDetails()){
                    if(Objects.equals(examResultDetails.getQuestionOrder(), getQuestionOrderFromFilename(filename))){
                        String filePath = mongoFileService.storeFile(file);
                        if(StringUtils.isNotEmpty(examResultDetails.getFilePath())){
                            examResultDetails.setFilePath(examResultDetails.getFilePath() + ";" + filePath);
                        }else{
                            examResultDetails.setFilePath(filePath);
                        }
                    }
                }
            }
        }

        List<ExamResultDetailsEntity> examResultDetailsEntities = new ArrayList<>();
        for(MyExamResultDetailsSaveReq examResultDetails: myExamResultSaveReq.getExamResultDetails()){
            examResultDetails.setExamResultId(examResultEntity.getId());
            ExamResultDetailsEntity examResultDetailsEntity = modelMapper.map(examResultDetails, ExamResultDetailsEntity.class);
            examResultDetailsEntities.add(examResultDetailsEntity);
        }
        examResultDetailsRepository.saveAll(examResultDetailsEntities);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Nộp bài thành công");
        return responseData;
    }
    private Integer getQuestionOrderFromFilename(String filename){
        if(StringUtils.isNotEmpty(filename)){
            String[] fileParts = filename.split("\\.");
            String[] subFileParts = fileParts[fileParts.length - 2].split("_");
            return Integer.parseInt(subFileParts[subFileParts.length-1]);
        }
        return null;
    }

    @Override
    public ResponseData<ExamMarkingDetailsRes> detailsExamMarking(String examStudentTestId) {
        ResponseData<ExamMarkingDetailsRes> responseData = new ResponseData<>();

        Optional<ExamMarkingDetailsResDB> examMarkingDetailsResDB = examRepository.detailsExamMarking(examStudentTestId);
        if(!examMarkingDetailsResDB.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại bài làm thi");
            return responseData;
        }

        ExamMarkingDetailsRes examMarkingDetailsRes = new ExamMarkingDetailsRes(examMarkingDetailsResDB.get(), null, modelMapper);

        examMarkingDetailsRes.setQuestionList(examTestRepository.getExamMarkingDetails(examStudentTestId));

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setData(examMarkingDetailsRes);
        responseData.setResultMsg("Success");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamResultEntity> markingExam(ExamMarkingSaveReq examMarkingSaveReq, MultipartFile[] files) {
        ResponseData<ExamResultEntity> responseData = new ResponseData<>();

        Optional<ExamResultEntity> examResultExist = examResultRepository.findById(examMarkingSaveReq.getExamResultId());
        if(!examResultExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại bài làm của học viên");
            return responseData;
        }
        ExamResultEntity examResult = examResultExist.get();
        examResult.setTotalScore(examMarkingSaveReq.getTotalScore());
        examResult.setComment(examMarkingSaveReq.getComment());
        examResultRepository.save(examResult);

        for(String filePath: examMarkingSaveReq.getCommentFilePathDeletes()){
            mongoFileService.deleteByPath(filePath);
        }

        if(files != null && files.length > 0){
            List<String> filePaths = mongoFileService.storeFiles(files);
            for(ExamMarkingDetailsSaveReq detailsSaveReq: examMarkingSaveReq.getExamResultDetails()){
                List<String> tmpFilePaths = new ArrayList<>();
                for(String filePath: filePaths){
                    if(Objects.equals(getExamResultDetailsIdFromFilepathComment(filePath), detailsSaveReq.getId())){
                        tmpFilePaths.add(filePath);
                    }
                }

                if(StringUtils.isNotBlank(detailsSaveReq.getCommentFilePath())){
                    if(!tmpFilePaths.isEmpty()){
                        detailsSaveReq.setCommentFilePath(detailsSaveReq.getCommentFilePath() +";"+ String.join(";", tmpFilePaths));
                    }
                }else{
                    detailsSaveReq.setCommentFilePath(String.join(";", tmpFilePaths));
                }
            }
        }

        List<ExamResultDetailsEntity> examResultDetailsEntities = new ArrayList<>();
        for(ExamMarkingDetailsSaveReq detailsSaveReq: examMarkingSaveReq.getExamResultDetails()){
            ExamResultDetailsEntity examResultDetailsEntity = modelMapper.map(detailsSaveReq, ExamResultDetailsEntity.class);
            examResultDetailsEntities.add(examResultDetailsEntity);
        }
        examResultDetailsRepository.saveAll(examResultDetailsEntities);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Chấm bài thành công");
        return responseData;
    }

    private String getExamResultDetailsIdFromFilepathComment(String filepath){
        if(StringUtils.isNotEmpty(filepath)){
            String[] filepaths = filepath.split("/");
            String[] fileParts = filepaths[filepaths.length - 1].split("\\.");
            String[] subFileParts = fileParts[0].split("_");
            return subFileParts[1];
        }
        return null;
    }
}
