package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamAccount;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelRepsonseExamClassDetail;
import com.hust.baseweb.dto.response.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ExamClassService {

    List<ExamClass> getAllExamClass();

    ExamClass createExamClass(String userLoginId, ModelCreateExamClass m);

    Object deleteAccounts(String userId, UUID examClassId);

    ApiResponse<ModelRepsonseExamClassDetail> getExamClassDetail(String userId, UUID examClassId);

    byte[] exportExamClass(String userId, UUID examClassId);

    Object resetPassword(String userId, UUID examClassId);

    Object generateAccounts(String userId, UUID examClassId);

    Object updateStatus(String userId, UUID examClassId, boolean enabled);

    ApiResponse<Void> deleteAccount(String userId, UUID examClassId, UUID accountId);

    ApiResponse<Void> resetPasswordForAccount(String userId, UUID examClassId, UUID accountId);

    ApiResponse<Void> updateStatusForAccount(String userId, UUID examClassId, UUID accountId, boolean enabled);

    ApiResponse<Void> generateAccountForSingle(String userId, UUID examClassId, UUID accountId);

    List<ExamAccount> importAccountsFromExcel(UUID examClassId, MultipartFile file) throws IOException;
}
