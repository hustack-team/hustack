package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.model.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
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

    CodeClassificationResult detectCodeAuthorshipOfSubmission(UUID submissionId);

    CodeClassificationResponse detectCodeAuthorship(List<CodeClassificationRequest> requests);

    void updateFinalSelectedSubmission(String userId,
                                       String contestId,
                                       String problemId,
                                       UUID newSubmissionId,
                                       UUID oldSubmissionId);
}
