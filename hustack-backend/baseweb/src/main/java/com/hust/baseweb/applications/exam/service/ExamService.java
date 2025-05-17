package com.hust.baseweb.applications.exam.service;

import com.hust.baseweb.applications.exam.entity.ExamEntity;
import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExamService {

    Page<ExamEntity> filter(Pageable pageable, ExamFilterReq examFilterReq);

    ResponseData<ExamDetailsRes> details(String id);

    ResponseData<List<ExamStudentResultDetailsRes>> detailStudentExam(String examExamTestId);

    ResponseData<ExamEntity> create(ExamSaveReq examSaveReq);

    ResponseData<ExamPreviewUpdateRes> previewUpdate(String id);

    ResponseData<ExamEntity> update(ExamSaveReq examSaveReq);

    ResponseData<ExamEntity> delete(String id);

    Page<MyExamFilterRes> filterMyExam(Pageable pageable, MyExamFilterReq myExamFilterReq);

    ResponseData<List<MyExamTestWithResultRes>> getListTestMyExam(String examTestIds);

    ResponseData<MyExamDetailsRes> detailsMyExam(String examStudentTestId);

    ResponseData<ExamResultEntity> doingMyExam(MyExamResultSaveReq myExamResultSaveReq, MultipartFile[] files);

    ResponseData<ExamMarkingDetailsRes> detailsExamMarking(String examStudentTestId);

    ResponseData<ExamResultEntity> markingExam(ExamMarkingSaveReq examMarkingSaveReq, MultipartFile[] files);
}
