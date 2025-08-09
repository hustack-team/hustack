package com.hust.baseweb.applications.exam.service;

import com.hust.baseweb.applications.exam.entity.ExamMonitorEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamMonitorSaveReq;

import java.util.List;

public interface ExamMonitorService {

    List<ExamMonitorEntity> findAllByExamResultId(String examResultId);
    ResponseData<ExamMonitorEntity> create(List<ExamMonitorSaveReq> list);
}
