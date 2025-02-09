package com.hust.baseweb.applications.exam.service;

import com.hust.baseweb.applications.exam.entity.ExamSubjectEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamSubjectDeleteReq;
import com.hust.baseweb.applications.exam.model.request.ExamSubjectFilterReq;
import com.hust.baseweb.applications.exam.model.request.ExamSubjectSaveReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExamSubjectService {

    Page<ExamSubjectEntity> filter(Pageable pageable, ExamSubjectFilterReq examSubjectFilterReq);

    List<ExamSubjectEntity> getAll();

    ResponseData<ExamSubjectEntity> create(ExamSubjectSaveReq examSubjectSaveReq);

    ResponseData<ExamSubjectEntity> update(ExamSubjectSaveReq examSubjectSaveReq);

    ResponseData<ExamSubjectEntity> delete(ExamSubjectDeleteReq examSubjectDeleteReq);
}
