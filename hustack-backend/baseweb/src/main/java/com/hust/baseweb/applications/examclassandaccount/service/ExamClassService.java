package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelRepsonseExamClassDetail;

import java.util.List;
import java.util.UUID;

public interface ExamClassService {

    List<ExamClass> getAllExamClass();

    ExamClass createExamClass(String userLoginId, ModelCreateExamClass m);

    boolean updateStatusExamClass(UUID examClassId, String status);

    boolean clearAccountExamClass(UUID examClassId);

    ModelRepsonseExamClassDetail getExamClassDetail(UUID examClassId);

    byte[] exportExamClass(UUID examClassId);
}
