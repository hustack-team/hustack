package com.hust.baseweb.applications.exam.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.exam.entity.*;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.*;
import com.hust.baseweb.applications.exam.repository.*;
import com.hust.baseweb.applications.exam.service.ExamService;
import com.hust.baseweb.applications.exam.service.ExamTestService;
import com.hust.baseweb.applications.exam.service.MongoFileService;
import com.hust.baseweb.applications.exam.utils.Constants;
import com.hust.baseweb.applications.exam.utils.DataUtils;
import com.hust.baseweb.applications.exam.utils.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        Optional<ExamEntity> examEntityExist = examRepository.findById(id);
        if(!examEntityExist.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn kỳ thi");
            return responseData;
        }

        ExamDetailsRes examDetailsRes = modelMapper.map(examEntityExist.get(), ExamDetailsRes.class);

        List<ExamTestDetailsRes> examTests = new ArrayList<>();
        examTests.add(examTestService.details(examDetailsRes.getExamTestId()).getData());
        examDetailsRes.setExamTests(examTests);

        examDetailsRes.setExamStudents(examStudentRepository.findAllWithResult(examDetailsRes.getId()));

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Success");
        responseData.setData(examDetailsRes);
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamEntity> create(ExamSaveReq examSaveReq) {
        ResponseData<ExamEntity> responseData = new ResponseData<>();

        Optional<ExamTestEntity> examTestEntity = examTestRepository.findById(examSaveReq.getExamTestId());
        if(!examTestEntity.isPresent()){
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

        if(!examSaveReq.getExamStudents().isEmpty()){
            for(ExamStudentEntity examStudent: examSaveReq.getExamStudents()){
                examStudent.setExamId(examEntity.getId());
                examStudent.setExamTestId(examSaveReq.getExamTestId());
            }
            examStudentRepository.saveAll(examSaveReq.getExamStudents());
        }

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Thêm mới kỳ thi thành công");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamEntity> update(ExamSaveReq examSaveReq) {
        ResponseData<ExamEntity> responseData = new ResponseData<>();

        Optional<ExamTestEntity> examTestEntity = examTestRepository.findById(examSaveReq.getExamTestId());
        if(!examTestEntity.isPresent()){
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

        if(!examSaveReq.getExamStudents().isEmpty()){
            for(ExamStudentEntity examStudent: examSaveReq.getExamStudents()){
                examStudent.setExamId(examEntity.getId());
                examStudent.setExamTestId(examSaveReq.getExamTestId());
            }
            examStudentRepository.saveAll(examSaveReq.getExamStudents());
        }

        if(!examSaveReq.getExamStudentDeletes().isEmpty()){
            examStudentRepository.deleteAll(examSaveReq.getExamStudentDeletes());
        }

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
    public ResponseData<MyExamDetailsRes> detailsMyExam(String examId, String examStudentId) {
        ResponseData<MyExamDetailsRes> responseData = new ResponseData<>();

        Optional<MyExamDetailsResDB> myExamDetailsResDB = examRepository.detailsMyExam(
            SecurityUtils.getUserLogin(),
            examId,
            examStudentId
        );
        if(!myExamDetailsResDB.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại bài thi");
            return responseData;
        }
        MyExamDetailsRes myExamDetailsRes = new MyExamDetailsRes(myExamDetailsResDB.get(), null, modelMapper);

        if(!DataUtils.stringIsNotNullOrEmpty(myExamDetailsRes.getExamResultId())){
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

        myExamDetailsRes.setQuestionList(examTestRepository.getMyExamQuestionDetails(myExamDetailsRes.getExamTestId(),
                                                                                     myExamDetailsRes.getExamStudentId()));

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setData(myExamDetailsRes);
        responseData.setResultMsg("Success");
        return responseData;
    }

    @Override
    @Transactional
    public ResponseData<ExamResultEntity> doingMyExam(MyExamResultSaveReq myExamResultSaveReq, MultipartFile[] files) {
        ResponseData<ExamResultEntity> responseData = new ResponseData<>();

        ExamResultEntity examResultEntity = modelMapper.map(myExamResultSaveReq, ExamResultEntity.class);
        examResultEntity = examResultRepository.save(examResultEntity);

        if(files != null){
            for(MultipartFile file: files){
                String filename = file.getOriginalFilename();
                for(MyExamResultDetailsSaveReq examResultDetails: myExamResultSaveReq.getExamResultDetails()){
                    if(Objects.equals(examResultDetails.getQuestionOrder(), getQuestionOrderFromFilename(filename))){
                        String filePath = mongoFileService.storeFile(file);
                        if(DataUtils.stringIsNotNullOrEmpty(examResultDetails.getFilePath())){
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
        if(DataUtils.stringIsNotNullOrEmpty(filename)){
            String[] fileParts = filename.split("\\.");
            String[] subFileParts = fileParts[fileParts.length - 2].split("_");
            return Integer.parseInt(subFileParts[subFileParts.length-1]);
        }
        return null;
    }

    @Override
    public ResponseData<ExamMarkingDetailsRes> detailsExamMarking(String examStudentId) {
        ResponseData<ExamMarkingDetailsRes> responseData = new ResponseData<>();

        Optional<ExamMarkingDetailsResDB> examMarkingDetailsResDB = examRepository.detailsExamMarking(examStudentId);
        if(!examMarkingDetailsResDB.isPresent()){
            responseData.setHttpStatus(HttpStatus.NOT_FOUND);
            responseData.setResultCode(HttpStatus.NOT_FOUND.value());
            responseData.setResultMsg("Chưa tồn tại bài làm thi");
            return responseData;
        }

        ExamMarkingDetailsRes examMarkingDetailsRes = new ExamMarkingDetailsRes(examMarkingDetailsResDB.get(), null, modelMapper);

        examMarkingDetailsRes.setQuestionList(examTestRepository.getExamMarkingDetails(examMarkingDetailsRes.getExamTestId(),
                                                                                          examMarkingDetailsRes.getExamStudentId()));

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
        if(DataUtils.stringIsNotNullOrEmpty(filepath)){
            String[] filepaths = filepath.split("/");
            String[] fileParts = filepaths[filepaths.length - 1].split("\\.");
            String[] subFileParts = fileParts[0].split("_");
            return subFileParts[1];
        }
        return null;
    }
}
