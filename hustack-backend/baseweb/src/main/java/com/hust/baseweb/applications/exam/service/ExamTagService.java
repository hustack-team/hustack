package com.hust.baseweb.applications.exam.service;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;

import java.util.List;

public interface ExamTagService {

    List<ExamTagEntity> getAll();

    ResponseData<ExamTagEntity> create(ExamTagSaveReq examTagSaveReq);

    ResponseData<ExamTagEntity> update(ExamTagSaveReq examTagSaveReq);
}
