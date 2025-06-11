package com.hust.baseweb.applications.exam.service;

import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamResultUpdateReq;

public interface ExamResultService {

    ResponseData<ExamResultEntity> update(ExamResultUpdateReq examResultUpdateReq);
}
