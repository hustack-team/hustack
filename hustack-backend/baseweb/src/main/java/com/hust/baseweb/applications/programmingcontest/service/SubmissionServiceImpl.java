package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import com.hust.baseweb.applications.programmingcontest.service.helper.cache.ProblemTestCaseServiceCache;
import com.hust.baseweb.config.HustackAiServiceConfig;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static com.hust.baseweb.applications.programmingcontest.entity.ContestEntity.*;
import static com.hust.baseweb.utils.CommonUtils.getClientIp;

@Slf4j
@Service
@Transactional
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

    ModelMapper mapper;

    TestCaseRepo testCaseRepo;

    ContestSubmissionTestCaseEntityRepo contestSubmissionTestCaseEntityRepo;

    WebClient.Builder webClientBuilder;

    HustackAiServiceConfig hustackAiServiceConfig;

    /**
     * @param model
     * @param file
     */
    @Override
    public Object submit(
        HttpServletRequest request,
        ModelContestSubmitProgramViaUploadFile model,
        MultipartFile file
    ) {
        ContestEntity contest = contestRepo.findContestByContestId(model.getContestId());
        if (contest == null) {
            return buildSubmissionResponseContestNotFound();
        }
        if (!ContestEntity.CONTEST_STATUS_RUNNING.equals(contest.getStatusId())) {
            return buildSubmissionResponseTimeOut();
        }

        List<String> languagesAllowed = contest.getListLanguagesAllowedInContest();
        if (!languagesAllowed.contains(model.getLanguage())) {
            return buildSubmissionNotLegalLanguage(model.getLanguage());
        }

        List<UserRegistrationContestEntity> userRegistrations = userRegistrationContestRepo
            .findUserRegistrationContestEntityByContestIdAndUserIdAndStatusAndRoleId(
                model.getContestId(),
                model.getUserId(),
                UserRegistrationContestEntity.STATUS_SUCCESSFUL,
                UserRegistrationContestEntity.ROLE_PARTICIPANT);

        if (!Boolean.TRUE.equals(contest.getContestPublic()) && userRegistrations.isEmpty()) {
            return buildSubmissionResponseNotRegistered();
        }

        for (UserRegistrationContestEntity ur : userRegistrations) {
            if (UserRegistrationContestEntity.PERMISSION_FORBIDDEN_SUBMIT.equals(ur.getPermissionId())) {
                return buildSubmissionResponseNoPermission();
            }
        }

        ContestProblem problem = contestProblemRepo.findByContestIdAndProblemId(
            model.getContestId(),
            model.getProblemId());

        if (problem == null) {
            return buildSubmissionResponseProblemNotFound();
        }
        if (ContestProblem.SUBMISSION_MODE_NOT_ALLOWED.equals(problem.getSubmissionMode())) {
            return buildSubmissionResponseSubmissionNotAllowed();
        }

        int numOfSubmissions = contestSubmissionRepo.countAllByContestIdAndUserIdAndProblemId(
            model.getContestId(),
            model.getUserId(),
            model.getProblemId());
        if (numOfSubmissions >= contest.getMaxNumberSubmissions()) {
            return buildSubmissionResponseReachMaxSubmission(contest.getMaxNumberSubmissions());
        }

        long submissionInterval = contest.getMinTimeBetweenTwoSubmissions();
        if (submissionInterval > 0) {
            Long lastSubmitTime = cacheService.findUserLastProblemSubmissionTimeInCache(
                model.getProblemId(),
                model.getUserId());
            if (lastSubmitTime != null) {
                long diffBetweenNowAndLastSubmit = new Date().getTime() - lastSubmitTime;
                if (diffBetweenNowAndLastSubmit < submissionInterval * 1000) {
                    return buildSubmissionResponseNotEnoughTimeBetweenSubmissions(submissionInterval);
                }
            }
            cacheService.addUserLastProblemSubmissionTimeToCache(model.getProblemId(), model.getUserId());
        }

        try (ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes())) {
            String source = IOUtils.toString(stream, StandardCharsets.UTF_8);

            if (StringUtils.isBlank(source)) {
                return buildSubmissionResponseSourceCodeRequired();
            }

            if (source.length() > contest.getMaxSourceCodeLength()) {
                return buildSubmissionResponseReachMaxSourceLength(
                    source.length(),
                    contest.getMaxSourceCodeLength());
            }

            ModelContestSubmission dto = new ModelContestSubmission(
                model.getContestId(),
                model.getProblemId(),
                source,
                model.getLanguage(),
                getClientIp(request));

            if (problem.getForbiddenInstructions() != null) {
                String[] forbiddenInstructions = problem.getForbiddenInstructions().split(",");
                for (String rawInstruction : forbiddenInstructions) {
                    String instruction = rawInstruction.trim();

                    if (!instruction.isEmpty() && source.contains(instruction)) {
                        return submitContestProblemNotExecuteDueToForbiddenInstructions(
                            dto,
                            model.getUserId(),
                            model.getSubmittedByUserId());
                    }
                }
            }

            ModelContestSubmissionResponse res;
            if (ContestEntity.CONTEST_SUBMISSION_ACTION_TYPE_STORE_AND_EXECUTE.equals(contest.getSubmissionActionType())) {
                if (ContestProblem.SUBMISSION_MODE_SOLUTION_OUTPUT.equals(problem.getSubmissionMode())) {
                    res = submitContestProblemStoreOnlyNotExecute(dto, model.getUserId(), model.getSubmittedByUserId());
                } else {
                    res = submitContestProblemTestCaseByTestCaseWithFile(
                        dto,
                        model.getUserId(),
                        model.getSubmittedByUserId());
                }
            } else {
                res = submitContestProblemStoreOnlyNotExecute(dto, model.getUserId(), model.getSubmittedByUserId());
            }

            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param principal
     * @param model
     * @param file
     * @return
     */
    @Override
    public Object managerSubmitCodeOfParticipant(
        HttpServletRequest request,
        Principal principal,
        ModelInputManagerSubmitCodeOfParticipant model,
        MultipartFile file
    ) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return buildSubmissionResponseInvalidFilename();
        }
        String[] s = filename.split("\\.");

        if (s.length < 2) {
            return buildSubmissionResponseInvalidFilename();
        }

        Tika tika = new Tika();
        String fileType = "";
        try {
            fileType = tika.detect(file.getInputStream());
        } catch (IOException ignored) {
        }

        String language;
        switch (fileType) {
            case "text/x-c" -> language = PROG_LANGUAGES_C;
            case "text/x-c++" -> language = PROG_LANGUAGES_CPP17;
            case "text/x-java-source" -> language = PROG_LANGUAGES_JAVA;
            case "text/x-python" -> language = PROG_LANGUAGES_PYTHON3;
            default -> {
                return buildSubmissionNotSupportedFileType();
            }
        }

        String[] s1 = s[0].split("_");
        if (s1.length < 2) {
            return buildSubmissionResponseInvalidFilename();
        }

        ContestProblem problem = contestProblemRepo.findByContestIdAndProblemRecode(model.getContestId(), s1[1].trim());
        if (problem == null) {
            return buildSubmissionResponseProblemNotFound();
        }

        ModelContestSubmitProgramViaUploadFile dto = new ModelContestSubmitProgramViaUploadFile(
            model.getContestId(),
            problem.getProblemId(),
            language,
            s1[0].trim(),
            principal.getName());
        return submit(request, dto, file);
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

        for (ContestSubmissionEntity submission : submissions) {// take the last submission in the sorted list
            problemTestCaseService.evaluateSubmission(submission, contest);
        }

        return null;
    }

    /**
     * @param userId
     * @param submissionId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public SubmissionDTO findById(String userId, UUID submissionId) {
        ContestSubmissionEntity submission = contestSubmissionRepo
            .findById(submissionId)
            .orElseThrow(() -> new EntityNotFoundException("Submission with ID " + submissionId + " not found"));

        if (!userId.equals(submission.getUserId())) {
            throw new EntityNotFoundException("Submission with ID " + submissionId + " not found");
        }

        ContestEntity contest = contestRepo.findContestByContestId(submission.getContestId());
        if (ContestEntity.PARTICIPANT_VIEW_SUBMISSION_MODE_DISABLED.equals(contest.getParticipantViewSubmissionMode())) {
            submission.setSourceCode("HIDDEN");
        }

        return mapper.map(submission, SubmissionDTO.class);
    }

    /**
     * @param submissionId
     * @return
     */
    @Override
    public CodeClassificationResult detectCodeAuthorshipOfSubmission(UUID submissionId) {
        ContestSubmissionEntity submission = contestSubmissionRepo
            .findById(submissionId)
            .orElseThrow(() -> new EntityNotFoundException("Submission with ID " + submissionId + " not found"));

        CodeClassificationResponse response = detectCodeAuthorship(List.of(new CodeClassificationRequest(
            submission.getSourceCode(),
            submission.getSourceCodeLanguage(),
            CodeClassificationMode.ADVANCED)));

        CodeClassificationResult result = response.results().get(0);
        submission.setCodeAuthorship("AI".equals(result.source()) ? result.aiModel() : result.source());
        contestSubmissionRepo.save(submission);
        return result;
    }

    /**
     * Determine the authorship of a code snippet (whether it is written by a human or AI).
     *
     * @param requests List of {@link CodeClassificationRequest} objects containing code, language, and mode.
     * @return {@link CodeClassificationResponse} containing the result of authorship analysis.
     */
    @Override
    public CodeClassificationResponse detectCodeAuthorship(List<CodeClassificationRequest> requests) {
        List<Map<String, Object>> requestBody = requests.stream().map(request -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", request.code());
            map.put("language", request.language());
            map.put("mode", request.mode().getValue());
            return map;
        }).collect(Collectors.toList());

        WebClient webClient = webClientBuilder.baseUrl(hustackAiServiceConfig.getUri()).build();
        return webClient.post()
                        .uri("/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(CodeClassificationResponse.class)
                        .doOnError(error -> log.error(error.getMessage()))
                        .block();
    }

    private ModelContestSubmissionResponse submitContestProblemTestCaseByTestCaseWithFile(
        ModelContestSubmission modelContestSubmission,
        String userId,
        String submittedByUserId
    ) {
        ContestSubmissionEntity submission = ContestSubmissionEntity.builder()
                                                                    .contestId(modelContestSubmission.getContestId())
                                                                    .problemId(modelContestSubmission.getProblemId())
                                                                    .sourceCode(modelContestSubmission.getSource())
                                                                    .sourceCodeLanguage(modelContestSubmission.getLanguage())
                                                                    .status(ContestSubmissionEntity.SUBMISSION_STATUS_EVALUATION_IN_PROGRESS)
                                                                    .point(0L)
                                                                    .userId(userId)
                                                                    .submittedByUserId(submittedByUserId)
                                                                    .runtime(0L)
                                                                    .createdByIp(modelContestSubmission.getCreatedByIp())
                                                                    .createdAt(new Date())
                                                                    .build();

        submission = contestSubmissionRepo.saveAndFlush(submission);

        problemTestCaseService.sendSubmissionToQueue(submission);
        return ModelContestSubmissionResponse.builder()
                                             .status("IN_PROGRESS")
                                             .message("Submission is being evaluated")
                                             .build();
    }

    private ModelContestSubmissionResponse submitContestProblemNotExecuteDueToForbiddenInstructions(
        ModelContestSubmission modelContestSubmission,
        String userName,
        String submittedByUserId
    ) {
        ContestSubmissionEntity submission = ContestSubmissionEntity.builder()
                                                                    .contestId(modelContestSubmission.getContestId())
                                                                    .problemId(modelContestSubmission.getProblemId())
                                                                    .sourceCode(modelContestSubmission.getSource())
                                                                    .sourceCodeLanguage(modelContestSubmission.getLanguage())
                                                                    .status(ContestSubmissionEntity.SUBMISSION_STATUS_NOT_EVALUATED_FORBIDDEN_INSTRUCTIONS)
                                                                    .point(0L)
                                                                    .userId(userName)
                                                                    .submittedByUserId(submittedByUserId)
                                                                    .runtime(0L)
                                                                    .createdAt(new Date())
                                                                    .build();

        contestSubmissionRepo.saveAndFlush(submission);
        return ModelContestSubmissionResponse.builder()
                                             .status("NO_EVALUATION_FORBIDDEN_INSTRUCTIONS")
                                             .message("Submission is not evaluated due to forbidden instructions")
                                             .build();
    }

    /**
     * @param dto
     * @param userId
     * @param submittedByUserId
     * @return
     */
    private ModelContestSubmissionResponse submitContestProblemStoreOnlyNotExecute(
        ModelContestSubmission dto,
        String userId,
        String submittedByUserId
    ) {
        String problemId = dto.getProblemId();
        String contestId = dto.getContestId();
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        ContestSubmissionEntity submission = ContestSubmissionEntity.builder()
                                                                    .contestId(contestId)
                                                                    .status(ContestSubmissionEntity.SUBMISSION_STATUS_NOT_AVAILABLE)
                                                                    .point(0L)
                                                                    .problemId(problemId)
                                                                    .userId(userId)
                                                                    .submittedByUserId(submittedByUserId)
                                                                    .testCasePass("")
                                                                    .sourceCode(dto.getSource())
                                                                    .sourceCodeLanguage(dto.getLanguage())
                                                                    .runtime(0L)
                                                                    .createdByIp(dto.getCreatedByIp())
                                                                    .createdAt(new Date())
                                                                    .build();
        submission = contestSubmissionRepo.save(submission);

        // generated test-case with empty result
        List<TestCaseEntity> testCases;
        if (ContestEntity.EVALUATE_USE_BOTH_PUBLIC_PRIVATE_TESTCASE_YES.equals(contest.getEvaluateBothPublicPrivateTestcase())) {
            testCases = testCaseRepo.findAllByProblemId(problemId);
        } else {
            testCases = testCaseRepo.findAllByProblemIdAndIsPublic(problemId, "N");
        }
        if (testCases == null) {
            testCases = new ArrayList<>();
        }

        for (TestCaseEntity testCase : testCases) {

            ContestSubmissionTestCaseEntity cste = ContestSubmissionTestCaseEntity.builder()
                                                                                  .contestId(contestId)
                                                                                  .problemId(problemId)
                                                                                  .contestSubmissionId(submission.getContestSubmissionId())
                                                                                  .testCaseId(testCase.getTestCaseId())
                                                                                  .submittedByUserLoginId(userId)
                                                                                  .point(0)
                                                                                  .status("N/A")
                                                                                  .participantSolutionOutput("")
                                                                                  .runtime(null)
                                                                                  .createdStamp(new Date())
                                                                                  .build();
            contestSubmissionTestCaseEntityRepo.save(cste);
        }

        return ModelContestSubmissionResponse.builder()
                                             .status("STORED")
                                             .testCasePass(submission.getTestCasePass())
                                             .runtime(0)
                                             .memoryUsage(submission.getMemoryUsage())
                                             .problemName("")
                                             .contestSubmissionID(submission.getContestSubmissionId())
                                             .submittedAt(submission.getCreatedAt())
                                             .score(0L)
                                             .numberTestCasePassed(0)
                                             .totalNumberTestCase(0)
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseContestNotFound() {
        return ModelContestSubmissionResponse.builder()
                                             .status("NOT_FOUND")
                                             .message("Contest not found")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseTimeOut() {
        return ModelContestSubmissionResponse.builder()
                                             .status("NOT_ALLOWED_TO_SUBMIT")
                                             .message("This contest does not allow submissions at the moment")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionNotLegalLanguage(String lang) {
        return ModelContestSubmissionResponse.builder()
                                             .status("NOT_ALLOWED_LANGUAGE")
                                             .message("The language " + lang + " is not allowed for this contest")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseProblemNotFound() {
        return ModelContestSubmissionResponse.builder()
                                             .status("NOT_FOUND")
                                             .message("Problem not found")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseInvalidFilename() {
        return ModelContestSubmissionResponse.builder()
                                             .status("INVALID")
                                             .message("Invalid filename")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionNotSupportedFileType() {
        return ModelContestSubmissionResponse.builder()
                                             .status("NOT_ALLOWED_FILE_TYPE")
                                             .message("File type is not allowed")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseNotRegistered() {
        return ModelContestSubmissionResponse.builder()
                                             .status("PARTICIPANT_NOT_REGISTERED_OR_APPROVED")
                                             .message("Participant is not registered or hasn't been approved yet")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseNoPermission() {
        return ModelContestSubmissionResponse.builder()
                                             .status("PARTICIPANT_HAS_NO_PERMISSION_TO_SUBMIT")
                                             .message("Participant does not have permission to submit")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseSubmissionNotAllowed() {
        return ModelContestSubmissionResponse.builder()
                                             .status("SUBMISSION_NOT_ALLOWED")
                                             .message("TThis problem is not open for submissions at the moment")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseReachMaxSubmission(int maxNumberSubmission) {
        return ModelContestSubmissionResponse.builder()
                                             .status("MAX_NUMBER_SUBMISSIONS_REACHED")
                                             .message("Maximum of " +
                                                      maxNumberSubmission +
                                                      " submissions reached. No further submissions allowed")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseSourceCodeRequired() {
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
                                             .message("Source code length exceeds the allowed limit of " +
                                                      maxLength +
                                                      " characters")
                                             .build();
    }

    private ModelContestSubmissionResponse buildSubmissionResponseNotEnoughTimeBetweenSubmissions(long interval) {
        return ModelContestSubmissionResponse.builder()
                                             .status("SUBMISSION_INTERVAL_VIOLATIONS")
                                             .message("Minimum interval of " +
                                                      interval +
                                                      " seconds is required between submissions")
                                             .build();
    }
}
