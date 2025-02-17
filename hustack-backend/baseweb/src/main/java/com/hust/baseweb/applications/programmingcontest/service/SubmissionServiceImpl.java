package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import com.hust.baseweb.applications.programmingcontest.entity.ContestProblem;
import com.hust.baseweb.applications.programmingcontest.entity.ContestSubmissionEntity;
import com.hust.baseweb.applications.programmingcontest.entity.UserRegistrationContestEntity;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.ContestProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ContestRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ContestSubmissionRepo;
import com.hust.baseweb.applications.programmingcontest.repo.UserRegistrationContestRepo;
import com.hust.baseweb.applications.programmingcontest.service.helper.cache.ProblemTestCaseServiceCache;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmissionServiceImpl implements SubmissionService {

    ContestRepo contestRepo;

    ContestProblemRepo contestProblemRepo;

    ProblemTestCaseService problemTestCaseService;

    UserRegistrationContestRepo userRegistrationContestRepo;

    ContestSubmissionRepo contestSubmissionRepo;

    ProblemTestCaseServiceCache cacheService;

    ContestService contestService;

    /**
     * @param userId
     * @param model
     * @param file
     */
    @Override
    public Object submitSubmission(String userId, ModelContestSubmitProgramViaUploadFile model, MultipartFile file) {
        ContestEntity contestEntity = contestRepo.findContestByContestId(model.getContestId());
        ContestProblem cp = contestProblemRepo.findByContestIdAndProblemId(model.getContestId(), model.getProblemId());
        List<String> languagesAllowed = contestEntity.getListLanguagesAllowedInContest();
        boolean languageOK = false;
        for (String l : languagesAllowed) {
            if (l.equals(model.getLanguage())) {
                languageOK = true;
                break;
            }
        }
        if (!languageOK) {
            ModelContestSubmissionResponse resp = buildSubmissionNotLegalLanguage(model.getLanguage());
            //log.info("contestSubmitProblemViaUploadFileV2, not legal language " + model.getLanguage());
            return resp;
        } else {
            //log.info("contestSubmitProblemViaUploadFileV2, legal language " + model.getLanguage());

        }

        if (!contestEntity.getStatusId().equals(ContestEntity.CONTEST_STATUS_RUNNING)) {
            ModelContestSubmissionResponse resp = buildSubmissionResponseTimeOut();
            return resp;
        }

        List<UserRegistrationContestEntity> userRegistrations = userRegistrationContestRepo
            .findUserRegistrationContestEntityByContestIdAndUserIdAndStatusAndRoleId(
                model.getContestId(),
                userId,
                UserRegistrationContestEntity.STATUS_SUCCESSFUL,
                UserRegistrationContestEntity.ROLE_PARTICIPANT);

        Boolean contestPublic = contestEntity.getContestPublic();
        if (!Boolean.TRUE.equals(contestPublic) || userRegistrations == null) {
            if (userRegistrations.isEmpty()) {
                ModelContestSubmissionResponse resp = buildSubmissionResponseNotRegistered();
                return resp;
            }
        }

        for (UserRegistrationContestEntity u : userRegistrations) {
            if (u.getPermissionId() != null
                && u.getPermissionId().equals(UserRegistrationContestEntity.PERMISSION_FORBIDDEN_SUBMIT)) {
                ModelContestSubmissionResponse resp = buildSubmissionResponseNoPermission();
                return resp;
            }
        }

        if (cp != null &&
            cp.getSubmissionMode() != null &&
            cp.getSubmissionMode().equals(ContestProblem.SUBMISSION_MODE_NOT_ALLOWED)) {
            ModelContestSubmissionResponse resp = buildSubmissionResponseSubmissionNotAllowed();
            return resp;
        }


        int numOfSubmissions = contestSubmissionRepo
            .countAllByContestIdAndUserIdAndProblemId(model.getContestId(), userId, model.getProblemId());
        if (numOfSubmissions >= contestEntity.getMaxNumberSubmissions()) {
            ModelContestSubmissionResponse resp = buildSubmissionResponseReachMaxSubmission(contestEntity.getMaxNumberSubmissions());
            return resp;
        }

        long submissionInterval = contestEntity.getMinTimeBetweenTwoSubmissions();
        if (submissionInterval > 0) {
            Date now = new Date();
            Long lastSubmitTime = cacheService.findUserLastProblemSubmissionTimeInCache(model.getProblemId(), userId);
            if (lastSubmitTime != null) {
                long diffBetweenNowAndLastSubmit = now.getTime() - lastSubmitTime;
                if (diffBetweenNowAndLastSubmit < submissionInterval * 1000) {
                    ModelContestSubmissionResponse resp = buildSubmissionResponseNotEnoughTimeBetweenSubmissions(
                        submissionInterval);
                    return resp;
                }
            }
            cacheService.addUserLastProblemSubmissionTimeToCache(model.getProblemId(), userId);
        }

        try (ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes())) {
            String source = IOUtils.toString(stream, StandardCharsets.UTF_8);

            if (StringUtils.isBlank(source)) {
                return buildSubmissionResponseMinSourceCodeRequired();
            }

            if (source.length() > contestEntity.getMaxSourceCodeLength()) {
                return buildSubmissionResponseReachMaxSourceLength(
                    source.length(),
                    contestEntity.getMaxSourceCodeLength());
            }

            ModelContestSubmission request = new ModelContestSubmission(
                model.getContestId(), model.getProblemId(),
                source, model.getLanguage());
            ModelContestSubmissionResponse resp;

            if (cp != null && cp.getForbiddenInstructions() != null) {
                log.info("contestSubmitProblemViaUploadFileV2, forbidden instructions = " +
                         cp.getForbiddenInstructions());
                String[] fis = cp.getForbiddenInstructions().split(",");
                boolean ok = true;
                if (fis != null) {
                    for (String fi : fis) {
                        String i = fi.trim();
                        log.info("contestSubmitProblemViaUploadFileV2, forbidden instructions i = " +
                                 i +
                                 " source = " +
                                 source);
                        if (i != null) {
                            if (!i.equals("") && i.length() > 0 && source.contains(i)) {
                                log.info("contestSubmitProblemViaUploadFileV2, has forbidden instructions i = " +
                                         i +
                                         " source = " +
                                         source);

                                ok = false;
                                break;
                            }
                        }
                    }
                }
                if (!ok) {
                    resp = problemTestCaseService.submitContestProblemNotExecuteDueToForbiddenInstructions(
                        request,
                        userId,
                        userId);

                    return resp;
                }
            }
            if (contestEntity.getSubmissionActionType()
                             .equals(ContestEntity.CONTEST_SUBMISSION_ACTION_TYPE_STORE_AND_EXECUTE)) {
                if (cp != null &&
                    cp.getSubmissionMode() != null &&
                    cp.getSubmissionMode().equals(ContestProblem.SUBMISSION_MODE_SOLUTION_OUTPUT)) {
                    resp = problemTestCaseService.submitContestProblemStoreOnlyNotExecute(request, userId, userId);
                } else {
                    log.info("contestSubmitProblemViaUploadFileV2, SUBMIT NORMAL");
                    resp = problemTestCaseService.submitContestProblemTestCaseByTestCaseWithFile(
                        request,
                        userId,
                        userId);
                }
            } else {
                resp = problemTestCaseService.submitContestProblemStoreOnlyNotExecute(request, userId, userId);
            }

            return resp;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param principal
     * @param model
     * @param file
     * @return
     */
    @Override
    public Object managerSubmitCodeOfParticipant(
        Principal principal,
        ModelInputManagerSubmitCodeOfParticipant model,
        MultipartFile file
    ) {
        ContestEntity contestEntity = contestRepo.findContestByContestId(model.getContestId());
        String filename = file.getOriginalFilename();
        log.info("ManagerSubmitCodeOfParticipant, filename = " + file.getOriginalFilename());
        String[] s = filename.split("\\.");
        log.info("ManagerSubmitCodeOfParticipant, extract from filename, s.length = " + s.length);
        if (s.length < 2) {
            return "Filename " + filename + " Invalid";
        }
        String language = s[1].trim();
        if (language.equals("cpp")) {
            language = ContestSubmissionEntity.LANGUAGE_CPP;
        } else if (language.equals("java")) {
            language = ContestSubmissionEntity.LANGUAGE_JAVA;
        } else if (language.equals("py")) {
            language = ContestSubmissionEntity.LANGUAGE_PYTHON;
        }

        String[] s1 = s[0].split("_");
        log.info("ManagerSubmitCodeOfParticipant, extract from filename, s[0] = " + s[0] + " s1 = " + s1.length);
        if (s1.length < 2) {
            ModelContestSubmissionResponse resp = buildSubmissionResponseInvalidFilename(filename);
            return resp;
        }
        String userId = s1[0].trim();
        String problemCode = s1[1].trim();
        String contestId = model.getContestId();
        String problemId = null;
        ContestProblem cp = contestProblemRepo.findByContestIdAndProblemRecode(contestId, problemCode);

        if (cp != null) {
            problemId = cp.getProblemId();
        } else {
            log.info("ManagerSubmitCodeOfParticipant, not found problem of code " + problemCode);
            ModelContestSubmissionResponse resp = buildSubmissionResponseProblemNotFound();
            return resp;
        }
        if (!contestEntity.getStatusId().equals(ContestEntity.CONTEST_STATUS_RUNNING)) {
            ModelContestSubmissionResponse resp = buildSubmissionResponseTimeOut();
            return resp;
        }

        boolean contestPublic = contestEntity.getContestPublic();
        List<UserRegistrationContestEntity> userRegistrations = userRegistrationContestRepo
            .findUserRegistrationContestEntityByContestIdAndUserIdAndStatusAndRoleId(
                model.getContestId(),
                userId,
                UserRegistrationContestEntity.STATUS_SUCCESSFUL,
                UserRegistrationContestEntity.ROLE_PARTICIPANT);
        if (!Boolean.TRUE.equals(contestPublic) || userRegistrations == null) {
            if (userRegistrations.isEmpty()) {
                ModelContestSubmissionResponse resp = buildSubmissionResponseNotRegistered();
                return resp;
            }

        }

        for (UserRegistrationContestEntity u : userRegistrations) {
            if (u.getPermissionId() != null
                && u.getPermissionId().equals(UserRegistrationContestEntity.PERMISSION_FORBIDDEN_SUBMIT)) {
                ModelContestSubmissionResponse resp = buildSubmissionResponseNoPermission();
                return resp;
            }
        }

        int numOfSubmissions = contestSubmissionRepo
            .countAllByContestIdAndUserIdAndProblemId(model.getContestId(), userId, problemId);
        if (numOfSubmissions >= contestEntity.getMaxNumberSubmissions()) {
            ModelContestSubmissionResponse resp = buildSubmissionResponseReachMaxSubmission(contestEntity.getMaxNumberSubmissions());
            return resp;
        }

        long submissionInterval = contestEntity.getMinTimeBetweenTwoSubmissions();
        if (submissionInterval > 0) {
            Date now = new Date();
            Long lastSubmitTime = cacheService.findUserLastProblemSubmissionTimeInCache(problemId, userId);
            if (lastSubmitTime != null) {
                long diffBetweenNowAndLastSubmit = now.getTime() - lastSubmitTime;
                if (diffBetweenNowAndLastSubmit < submissionInterval * 1000) {
                    ModelContestSubmissionResponse resp = buildSubmissionResponseNotEnoughTimeBetweenSubmissions(
                        submissionInterval);
                    return resp;
                }
            }
            cacheService.addUserLastProblemSubmissionTimeToCache(problemId, userId);
        }

        try (ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes())) {
            String source = IOUtils.toString(stream, StandardCharsets.UTF_8);

            if (source.length() > contestEntity.getMaxSourceCodeLength()) {
                ModelContestSubmissionResponse resp = buildSubmissionResponseReachMaxSourceLength(
                    source.length(),
                    contestEntity.getMaxSourceCodeLength());
                return resp;
            }
            ModelContestSubmission request = new ModelContestSubmission(
                model.getContestId(), problemId,
                source, language);
            ModelContestSubmissionResponse resp;
            if (contestEntity.getSubmissionActionType()
                             .equals(ContestEntity.CONTEST_SUBMISSION_ACTION_TYPE_STORE_AND_EXECUTE)) {
                if (cp.getSubmissionMode() != null &&
                    cp.getSubmissionMode().equals(ContestProblem.SUBMISSION_MODE_SOLUTION_OUTPUT)) {
                    resp = problemTestCaseService.submitContestProblemStoreOnlyNotExecute(
                        request,
                        userId,
                        principal.getName());
                } else {
                    resp = problemTestCaseService.submitContestProblemTestCaseByTestCaseWithFile(
                        request,
                        userId,
                        principal.getName());
                }
            } else {
                resp = problemTestCaseService.submitContestProblemStoreOnlyNotExecute(
                    request,
                    userId,
                    principal.getName());
            }
            log.info("ManagerSubmitCodeOfParticipant, submitted successfully");
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param contestId
     * @return
     */
    @Override
    public ModelEvaluateBatchSubmissionResponse judgeAllSubmissionsOfContest(String contestId) {
        List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestIdAndStatus(
            contestId,
            ContestSubmissionEntity.SUBMISSION_STATUS_EVALUATION_IN_PROGRESS);
        ContestEntity contest = contestService.findContestWithCache(contestId);

        for (ContestSubmissionEntity sub : submissions) {// take the last submission in the sorted list
            problemTestCaseService.evaluateSubmission(sub, contest);
        }

        return null;
    }

    private ModelContestSubmissionResponse buildSubmissionResponseTimeOut() {
        return ModelContestSubmissionResponse.builder()
                                             .status("TIME_OUT")
                                             .testCasePass("0")
                                             .runtime(0L)
                                             .memoryUsage((float) 0)
                                             .problemName("")
                                             .contestSubmissionID(null)
                                             .submittedAt(null)
                                             .score(0L)
                                             .numberTestCasePassed(0)
                                             .totalNumberTestCase(0)
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionNotLegalLanguage(String lang) {
        return ModelContestSubmissionResponse.builder()
                                             //.status("ILLEGAL LANGUAGE " + lang)
                                             .status("ILLEGAL_LANGUAGE")
                                             .message("Illegal language " + lang)
                                             .testCasePass("0")
                                             .runtime(0L)
                                             .memoryUsage((float) 0)
                                             .problemName("")
                                             .contestSubmissionID(null)
                                             .submittedAt(null)
                                             .score(0L)
                                             .numberTestCasePassed(0)
                                             .totalNumberTestCase(0)
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseProblemNotFound() {
        return ModelContestSubmissionResponse.builder()
                                             .status("PROBLEM_NO_FOUND")
                                             .testCasePass("0")
                                             .runtime(0L)
                                             .memoryUsage((float) 0)
                                             .problemName("")
                                             .contestSubmissionID(null)
                                             .submittedAt(null)
                                             .score(0L)
                                             .numberTestCasePassed(0)
                                             .totalNumberTestCase(0)
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseInvalidFilename(String fn) {
        return ModelContestSubmissionResponse.builder()
                                             .status("Invalid filename " + fn)
                                             .testCasePass("0")
                                             .runtime(0L)
                                             .memoryUsage((float) 0)
                                             .problemName("")
                                             .contestSubmissionID(null)
                                             .submittedAt(null)
                                             .score(0L)
                                             .numberTestCasePassed(0)
                                             .totalNumberTestCase(0)
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseNotRegistered() {
        return ModelContestSubmissionResponse.builder()
                                             .status("PARTICIPANT_NOT_APPROVED_OR_REGISTERED")
                                             .message("Participant is not approved or not registered")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseNoPermission() {
        return ModelContestSubmissionResponse.builder()
                                             .status("PARTICIPANT_HAS_NOT_PERMISSION_TO_SUBMIT")
                                             .message("Participant has no permission to submit")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseSubmissionNotAllowed() {
        return ModelContestSubmissionResponse.builder()
                                             .status("SUBMISSION_NOT_ALLOWED")
                                             .message(
                                                 "This problem is not opened for submitting solution")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseReachMaxSubmission(int maxNumberSubmission) {
        return ModelContestSubmissionResponse.builder()
                                             .status("MAX_NUMBER_SUBMISSIONS_REACHED")
                                             .message("Maximum Number of Submissions " + maxNumberSubmission
                                                      + " Reached! Cannot submit more")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseMinSourceCodeRequired(

    ) {
        return ModelContestSubmissionResponse.builder()
                                             .status("SOURCE_CODE_REQUIRED")
                                             .message("Source code is required")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseReachMaxSourceLength(
        int sourceLength,
        int maxLength
    ) {
        return ModelContestSubmissionResponse.builder()
                                             .status("MAX_SOURCE_CODE_LENGTH_VIOLATIONS")
                                             .message("Max source code length violations " + sourceLength + " exceeded "
                                                      + maxLength + " ")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseNotEnoughTimeBetweenSubmissions(long interval) {
        return ModelContestSubmissionResponse.builder()
                                             .status("SUBMISSION_INTERVAL_VIOLATIONS")
                                             .message("Not enough time between 2 submissions (" + interval + "s) ")
                                             .build();
    }
}
