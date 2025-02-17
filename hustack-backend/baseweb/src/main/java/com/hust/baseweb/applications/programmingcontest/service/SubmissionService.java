package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.model.ModelContestSubmitProgramViaUploadFile;
import com.hust.baseweb.applications.programmingcontest.model.ModelEvaluateBatchSubmissionResponse;
import com.hust.baseweb.applications.programmingcontest.model.ModelInputManagerSubmitCodeOfParticipant;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface SubmissionService {

    Object submitSubmission(String userId, ModelContestSubmitProgramViaUploadFile model, MultipartFile file);

    Object managerSubmitCodeOfParticipant(
        Principal principal,
        ModelInputManagerSubmitCodeOfParticipant model,
        MultipartFile file
    );

    ModelEvaluateBatchSubmissionResponse judgeAllSubmissionsOfContest(String contestId);
}
