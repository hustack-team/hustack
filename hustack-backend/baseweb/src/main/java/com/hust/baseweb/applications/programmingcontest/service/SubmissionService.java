package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.model.ModelContestSubmitProgramViaUploadFile;
import com.hust.baseweb.applications.programmingcontest.model.ModelEvaluateBatchSubmissionResponse;
import com.hust.baseweb.applications.programmingcontest.model.ModelInputManagerSubmitCodeOfParticipant;
import com.hust.baseweb.applications.programmingcontest.model.SubmissionDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

public interface SubmissionService {

    Object submit(HttpServletRequest request, ModelContestSubmitProgramViaUploadFile model, MultipartFile file);

    Object managerSubmitCodeOfParticipant(
        HttpServletRequest request,
        Principal principal,
        ModelInputManagerSubmitCodeOfParticipant model,
        MultipartFile file
    );

    ModelEvaluateBatchSubmissionResponse judgeAllSubmissionsOfContest(String contestId);

    SubmissionDTO findById(String userId, UUID submissionId);
}
