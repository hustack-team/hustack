package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.notifications.service.NotificationsService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.exception.MiniLeetCodeException;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.ContestProblemModelResponse;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import com.hust.baseweb.applications.programmingcontest.service.helper.cache.ProblemTestCaseServiceCache;
import com.hust.baseweb.applications.programmingcontest.utils.ComputerLanguage;
import com.hust.baseweb.applications.programmingcontest.utils.ContestProblemPermissionUtil;
import com.hust.baseweb.applications.programmingcontest.utils.DateTimeUtils;
import com.hust.baseweb.applications.programmingcontest.utils.codesimilaritycheckingalgorithms.CodeSimilarityCheck;
import com.hust.baseweb.entity.UserLogin;
import com.hust.baseweb.model.SubmissionFilter;
import com.hust.baseweb.model.TestCaseFilter;
import com.hust.baseweb.repo.UserLoginRepo;
import com.hust.baseweb.service.UserService;
import com.hust.baseweb.utils.CommonUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.soict.judge0client.config.Judge0Config;
import vn.edu.hust.soict.judge0client.entity.Judge0Submission;
import vn.edu.hust.soict.judge0client.service.Judge0Service;
import vn.edu.hust.soict.judge0client.utils.Judge0Utils;

import java.util.*;
import java.util.stream.Collectors;

import static com.hust.baseweb.applications.programmingcontest.entity.ContestEntity.PROG_LANGUAGES_JAVA;
import static com.hust.baseweb.applications.programmingcontest.entity.ContestEntity.PROG_LANGUAGES_PYTHON3;
import static com.hust.baseweb.config.rabbitmq.RabbitConfig.EXCHANGE;
import static com.hust.baseweb.config.rabbitmq.RabbitRoutingKey.JUDGE_PROBLEM;
import static com.hust.baseweb.config.rabbitmq.RabbitRoutingKey.MULTI_THREADED_PROGRAM;
import static com.hust.baseweb.utils.PdfUtils.exportPdf;

@Slf4j
@Service
@AllArgsConstructor(onConstructor_ = {@Autowired})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProblemTestCaseServiceImpl implements ProblemTestCaseService {

    public static final Integer MAX_SUBMISSIONS_CHECK_SIMILARITY = 1000;

    ProblemRepo problemRepo;

    TeacherGroupRelationRepository teacherGroupRelationRepository;

    TestCaseRepo testCaseRepo;

    UserLoginRepo userLoginRepo;

    ContestRepo contestRepo;

    ContestPagingAndSortingRepo contestPagingAndSortingRepo;

    ContestSubmissionRepo contestSubmissionRepo;

    UserRegistrationContestRepo userRegistrationContestRepo;

    NotificationsService notificationsService;

    UserRegistrationContestPagingAndSortingRepo userRegistrationContestPagingAndSortingRepo;

    ContestSubmissionPagingAndSortingRepo contestSubmissionPagingAndSortingRepo;

    ContestSubmissionTestCaseEntityRepo contestSubmissionTestCaseEntityRepo;

    UserService userService;

    ContestRoleRepo contestRoleRepo;

    CodePlagiarismRepo codePlagiarismRepo;

    ContestSubmissionHistoryRepo contestSubmissionHistoryRepo;

    ContestProblemRepo contestProblemRepo;

    UserContestProblemRoleRepo userContestProblemRoleRepo;

    TagRepo tagRepo;

    ContestService contestService;

    TestCaseService testCaseService;

    RabbitTemplate rabbitTemplate;

    ProblemTestCaseServiceCache cacheService;

    ContestUserParticipantGroupRepo contestUserParticipantGroupRepo;

    UserRegistrationContestService userRegistrationContestService;

    Judge0Service judge0Service;

    Judge0Utils judge0Utils;

    ContestProblemPermissionUtil contestProblemPermissionUtil;

    private float getTimeLimitByLanguage(ProblemEntity problem, String language) {
        float timeLimit;
        switch (language) {
            case ContestSubmissionEntity.LANGUAGE_CPP:
                timeLimit = problem.getTimeLimitCPP();
                break;
            case PROG_LANGUAGES_JAVA:
                timeLimit = problem.getTimeLimitJAVA();
                break;
            case PROG_LANGUAGES_PYTHON3:
                timeLimit = problem.getTimeLimitPYTHON();
                break;
            default:
                timeLimit = problem.getTimeLimitCPP();
        }
        return timeLimit;
    }

//    @Override
//    public ModelGetTestCaseResultResponse getTestCaseResult(
//        String problemId,
//        String userName,
//        ModelGetTestCaseResult modelGetTestCaseResult
//    ) throws Exception {
//        ProblemEntity problemEntity = problemRepo.findByProblemId(problemId);
//        String tempName = tempDir.createRandomScriptFileName(userName +
//                                                             "-" +
//                                                             problemEntity.getProblemId() +
//                                                             "-" +
//                                                             problemEntity.getCorrectSolutionLanguage());
//        String output = runCode(
//            problemEntity.getCorrectSolutionSourceCode(),
//            problemEntity.getCorrectSolutionLanguage(),
//            tempName,
//            modelGetTestCaseResult.getTestcase(),
//            getTimeLimitByLanguage(problemEntity, problemEntity.getCorrectSolutionLanguage()),
//            "Correct Solution Language Not Found");
//        if (output.contains("Time Limit Exceeded")) {
//            return ModelGetTestCaseResultResponse.builder()
//                                                 .result("")
//                                                 .status("Time Limit Exceeded")
//                                                 .build();
//        }
//        output = output.substring(0, output.length() - 1);
//        int lastLinetIndexExpected = output.lastIndexOf("\n");
//        output = output.substring(0, lastLinetIndexExpected);
////        output = output.replaceAll("\n", "");
//        //    log.info("output {}", output);
//        return ModelGetTestCaseResultResponse.builder()
//                                             .result(output)
//                                             .status("ok")
//                                             .build();
//    }

    /**
     * @param modelCheckCompile
     * @param userName
     * @return
     * @throws Exception
     */
    @Override
    public ModelCheckCompileResponse checkCompile(
        ModelCheckCompile modelCheckCompile,
        String userName
    ) throws Exception {
        int languageId;
        String compilerOptions = null;
        switch (ComputerLanguage.Languages.valueOf(modelCheckCompile.getComputerLanguage())) {
            case C:
                languageId = 50;
                compilerOptions = "-std=c17 -w -O2 -lm -fmax-errors=3";
                break;
            case CPP11:
                languageId = 54;
                compilerOptions = "-std=c++11 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
                break;
            case CPP14:
                languageId = 54;
                compilerOptions = "-std=c++14 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
                break;
            case CPP:
            case CPP17:
                languageId = 54;
                compilerOptions = "-std=c++17 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
                break;
            case JAVA:
                languageId = 62;
                break;
            case PYTHON3:
                languageId = 71;
                break;
            default:
                throw new Exception("Language not supported");
        }

        // Actually only need to compile, not run, so set time limit small enough
        String sourceCode = modelCheckCompile.getSource();
        Judge0Config.ServerConfig serverConfig = judge0Utils.getServerConfig(languageId, sourceCode);
        Judge0Submission submission = Judge0Submission.builder()
                                                      .sourceCode(sourceCode)
                                                      .languageId(languageId)
                                                      .compilerOptions(compilerOptions)
                                                      .commandLineArguments(null)
                                                      .cpuTimeLimit(0.05F)
                                                      .cpuExtraTime(0.05F)
                                                      .wallTimeLimit(1.0F)
                                                      .memoryLimit(Float.valueOf(serverConfig
                                                                                     .getSubmission()
                                                                                     .getMaxMemoryLimit()))
                                                      .stackLimit(serverConfig.getSubmission().getMaxStackLimit())
                                                      .maxProcessesAndOrThreads(judge0Utils.getMaxProcessesAndOrThreads(
                                                          languageId,
                                                          sourceCode))
                                                      .enablePerProcessAndThreadTimeLimit(false)
                                                      .enablePerProcessAndThreadMemoryLimit(false)
                                                      .maxFileSize(serverConfig.getSubmission().getMaxMaxFileSize())
                                                      .redirectStderrToStdout(false)
                                                      .enableNetwork(false)
                                                      .numberOfRuns(1)
                                                      .build();

        submission = judge0Service.createASubmission(serverConfig, submission, true, true);
        submission.decodeBase64();

        return ModelCheckCompileResponse.builder()
                                        .status(submission.getStatus().getDescription())
                                        .message(submission.getCompileOutput())
                                        .build();
    }

//    @Override
//    public TestCaseEntity saveTestCase(String problemId, ModelSaveTestcase modelSaveTestcase) {
//
//        TestCaseEntity testCaseEntity = TestCaseEntity.builder()
//                                                      .correctAnswer(modelSaveTestcase.getResult())
//                                                      .testCase(modelSaveTestcase.getInput())
//                                                      .testCasePoint(modelSaveTestcase.getPoint())
//                                                      .problemId(problemId)
//                                                      .isPublic(modelSaveTestcase.getIsPublic())
//                                                      .build();
//        return testCaseService.saveTestCaseWithCache(testCaseEntity);
//    }

    @Transactional
    @Override
    public ContestEntity createContest(ModelCreateContest modelCreateContest, String userName) throws Exception {
        try {
            String contestId = modelCreateContest.getContestId().trim();
            ContestEntity contestEntityExist = contestRepo.findContestByContestId(contestId);
            if (contestEntityExist != null) {
                throw new MiniLeetCodeException("Contest is already exist");
            }
            ContestEntity contestEntity = ContestEntity.builder()
                                                       .contestId(contestId)
                                                       .contestName(modelCreateContest.getContestName())
                                                       .contestSolvingTime(modelCreateContest.getContestTime())
                                                       .countDown(modelCreateContest.getCountDownTime())
                                                       .startedAt(modelCreateContest.getStartedAt())
                                                       .startedCountDownTime(DateTimeUtils.minusMinutesDate(
                                                           modelCreateContest.getStartedAt(),
                                                           modelCreateContest.getCountDownTime()))
                                                       .endTime(DateTimeUtils.addMinutesDate(
                                                           modelCreateContest.getStartedAt(),
                                                           modelCreateContest.getContestTime()))
                                                       .userId(userName)
                                                       .statusId(ContestEntity.CONTEST_STATUS_CREATED)
                                                       .maxNumberSubmissions(modelCreateContest.getMaxNumberSubmissions())
                                                       .maxSourceCodeLength(modelCreateContest.getMaxSourceCodeLength())
                                                       .minTimeBetweenTwoSubmissions(modelCreateContest.getMinTimeBetweenTwoSubmissions())
                                                       .judgeMode(ContestEntity.ASYNCHRONOUS_JUDGE_MODE_QUEUE)
                                                       .submissionActionType(ContestEntity.CONTEST_SUBMISSION_ACTION_TYPE_STORE_AND_EXECUTE)
                                                       .problemDescriptionViewType(ContestEntity.CONTEST_PROBLEM_DESCRIPTION_VIEW_TYPE_VISIBLE)
                                                       .participantViewResultMode(ContestEntity.CONTEST_PARTICIPANT_VIEW_TESTCASE_DETAIL_ENABLED)
                                                       .evaluateBothPublicPrivateTestcase(ContestEntity.EVALUATE_USE_BOTH_PUBLIC_PRIVATE_TESTCASE_YES)
                                                       .sendConfirmEmailUponSubmission(ContestEntity.SEND_CONFIRM_EMAIL_UPON_SUBMISSION_NO)
                                                       .contestShowTag(ContestEntity.CONTEST_SHOW_TAG_PROBLEMS_NO)
                                                       .createdAt(new Date())
                                                       //.contestType(modelCreateContest.getContestType())
                                                       .contestType(ContestEntity.CONTEST_TYPE_TRAINING_NO_EVALUATION)
                                                       .build();

/*
            if (modelCreateContest.getStartedAt() != null) {
                contestEntity = ContestEntity.builder()
                                             .contestId(contestId)
                                             .contestName(modelCreateContest.getContestName())
                                             .contestSolvingTime(modelCreateContest.getContestTime())
//                                             .problems(problemEntities)
                                             .countDown(modelCreateContest.getCountDownTime())
                                             .startedAt(modelCreateContest.getStartedAt())
                                             .startedCountDownTime(DateTimeUtils.minusMinutesDate(
                                                 modelCreateContest.getStartedAt(),
                                                 modelCreateContest.getCountDownTime()))
                                             .endTime(DateTimeUtils.addMinutesDate(
                                                 modelCreateContest.getStartedAt(),
                                                 modelCreateContest.getContestTime()))
                                             .userId(userName)
                                             .statusId(ContestEntity.CONTEST_STATUS_CREATED)
                                             .maxNumberSubmissions(modelCreateContest.getMaxNumberSubmissions())
                                             .maxSourceCodeLength(modelCreateContest.getMaxSourceCodeLength())
                                             .minTimeBetweenTwoSubmissions(modelCreateContest.getMinTimeBetweenTwoSubmissions())
                                             .judgeMode(ContestEntity.ASYNCHRONOUS_JUDGE_MODE_QUEUE)
                                             .submissionActionType(ContestEntity.CONTEST_SUBMISSION_ACTION_TYPE_STORE_AND_EXECUTE)
                                             .problemDescriptionViewType(ContestEntity.CONTEST_PROBLEM_DESCRIPTION_VIEW_TYPE_VISIBLE)
                                             //.participantViewResultMode(ContestEntity.CONTEST_PARTICIPANT_VIEW_MODE_SEE_CORRECT_ANSWER)
                                             .participantViewResultMode(ContestEntity.CONTEST_PARTICIPANT_VIEW_TESTCASE_DETAIL_ENABLED)
                                             //.evaluateBothPublicPrivateTestcase(ContestEntity.EVALUATE_USE_BOTH_PUBLIC_PRIVATE_TESTCASE_NO)
                                             .evaluateBothPublicPrivateTestcase(ContestEntity.EVALUATE_USE_BOTH_PUBLIC_PRIVATE_TESTCASE_YES)

                                             .createdAt(new Date())
                                             .build();
            } else {
                contestEntity = ContestEntity.builder()
                                             .contestId(contestId)
                                             .contestName(modelCreateContest.getContestName())
                                             .contestSolvingTime(modelCreateContest.getContestTime())
//                                             .problems(problemEntities)
                                             .countDown(modelCreateContest.getCountDownTime())
                                             .userId(userName)
                                             .statusId(ContestEntity.CONTEST_STATUS_CREATED)
                                             .maxNumberSubmissions(modelCreateContest.getMaxNumberSubmissions())
                                             .maxSourceCodeLength(modelCreateContest.getMaxSourceCodeLength())
                                             .createdAt(new Date())
                                             .build();
            }
*/
            contestEntity = contestService.saveContestWithCache(contestEntity);

            // create corresponding entities in ContestRole
            ContestRole contestRole = new ContestRole();
            contestRole.setContestId(modelCreateContest.getContestId());
            contestRole.setUserLoginId(userName);
            Date fromDate = new Date();
            contestRole.setRoleId(ContestRole.CONTEST_ROLE_OWNER);
            contestRole.setFromDate(fromDate);
            contestRoleRepo.save(contestRole);

            contestRole = new ContestRole();
            contestRole.setContestId(modelCreateContest.getContestId());
            contestRole.setUserLoginId(userName);
            contestRole.setRoleId(ContestRole.CONTEST_ROLE_MANAGER);
            contestRole.setFromDate(fromDate);
            contestRoleRepo.save(contestRole);

            contestRole = new ContestRole();
            contestRole.setContestId(modelCreateContest.getContestId());
            contestRole.setUserLoginId(userName);
            contestRole.setRoleId(ContestRole.CONTEST_ROLE_PARTICIPANT);
            contestRole.setFromDate(fromDate);
            contestRoleRepo.save(contestRole);

            // create correspoding entities in UserRegistrationContestEntity
            UserRegistrationContestEntity urc = new UserRegistrationContestEntity();
            urc.setContestId(modelCreateContest.getContestId());
            urc.setRoleId(UserRegistrationContestEntity.ROLE_OWNER);
            urc.setUserId(userName);
            urc.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
            userRegistrationContestRepo.save(urc);

            urc = new UserRegistrationContestEntity();
            urc.setContestId(modelCreateContest.getContestId());
            urc.setRoleId(UserRegistrationContestEntity.ROLE_MANAGER);
            urc.setUserId(userName);
            urc.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
            userRegistrationContestRepo.save(urc);

            urc = new UserRegistrationContestEntity();
            urc.setContestId(modelCreateContest.getContestId());
            urc.setRoleId(UserRegistrationContestEntity.ROLE_PARTICIPANT);
            urc.setUserId(userName);
            urc.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
            userRegistrationContestRepo.save(urc);

            String admin = "admin";
            UserLogin u = userLoginRepo.findByUserLoginId(admin);
            if (u != null && !"admin".equals(userName)) {
                urc = new UserRegistrationContestEntity();
                urc.setContestId(modelCreateContest.getContestId());
                urc.setRoleId(UserRegistrationContestEntity.ROLE_OWNER);
                urc.setUserId(admin);
                urc.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
                userRegistrationContestRepo.save(urc);

                urc = new UserRegistrationContestEntity();
                urc.setContestId(modelCreateContest.getContestId());
                urc.setRoleId(UserRegistrationContestEntity.ROLE_MANAGER);
                urc.setUserId(admin);
                urc.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
                userRegistrationContestRepo.save(urc);

                urc = new UserRegistrationContestEntity();
                urc.setContestId(modelCreateContest.getContestId());
                urc.setRoleId(UserRegistrationContestEntity.ROLE_PARTICIPANT);
                urc.setUserId(admin);
                urc.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
                userRegistrationContestRepo.save(urc);

                //push notification to admin
                notificationsService.create(
                    userName, u.getUserLoginId(),
                    userName + " has created a contest " +
                    modelCreateContest.getContestId()
                    , "");

            }
            return contestEntity;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public ContestEntity updateContest(
        ModelUpdateContest modelUpdateContest,
        String userName,
        String contestId
    ) throws Exception {
        ContestEntity contestEntityExist = contestRepo.findContestByContestId(contestId);
        if (contestEntityExist == null) {
            throw new MiniLeetCodeException("Contest does not exist");
        }
        log.info("updateContest, languages = " + modelUpdateContest.getLanguagesAllowed());
        List<UserRegistrationContestEntity> L = userRegistrationContestRepo
            .findByContestIdAndUserIdAndStatus(
                contestId,
                userName,
                UserRegistrationContestEntity.STATUS_SUCCESSFUL);
        boolean canUpdate = false;
        for (UserRegistrationContestEntity u : L) {
            if (u.getRoleId().equals(UserRegistrationContestEntity.ROLE_MANAGER) ||
                u.getRoleId().equals(UserRegistrationContestEntity.ROLE_OWNER)) {
                canUpdate = true;
                break;
            }
        }
        if (!canUpdate) {
            throw new MiniLeetCodeException("You don't have privileged");
        }

        ContestEntity contestEntity = ContestEntity.builder()
                                                   .contestId(contestId)
                                                   .contestName(modelUpdateContest.getContestName())
                                                   .contestSolvingTime(modelUpdateContest.getContestSolvingTime())
                                                   .problems(contestEntityExist.getProblems())
                                                   .userId(contestEntityExist.getUserId())
//                                                   .countDown(modelUpdateContest.getCountDownTime())
//                                                   .startedAt(modelUpdateContest.getStartedAt())
//                                                   .startedCountDownTime(DateTimeUtils.minusMinutesDate(
//                                                       modelUpdateContest.getStartedAt(),
//                                                       modelUpdateContest.getCountDownTime()))
//                                                   .endTime(DateTimeUtils.addMinutesDate(
//                                                       modelUpdateContest.getStartedAt(),
//                                                       modelUpdateContest.getContestSolvingTime()))
                                                   .statusId(modelUpdateContest.getStatusId())
                                                   .submissionActionType(modelUpdateContest.getSubmissionActionType())
                                                   .maxNumberSubmissions(modelUpdateContest.getMaxNumberSubmission())
                                                   .participantViewResultMode(modelUpdateContest.getParticipantViewResultMode())
                                                   .problemDescriptionViewType(modelUpdateContest.getProblemDescriptionViewType())
                                                   .maxSourceCodeLength(modelUpdateContest.getMaxSourceCodeLength())
                                                   .evaluateBothPublicPrivateTestcase(modelUpdateContest.getEvaluateBothPublicPrivateTestcase())
                                                   .minTimeBetweenTwoSubmissions(modelUpdateContest.getMinTimeBetweenTwoSubmissions())
                                                   .judgeMode(modelUpdateContest.getJudgeMode())
                                                   .sendConfirmEmailUponSubmission(modelUpdateContest.getSendConfirmEmailUponSubmission())
                                                   .participantViewSubmissionMode(modelUpdateContest.getParticipantViewSubmissionMode())
                                                   .languagesAllowed(String.join(
                                                       ",",
                                                       modelUpdateContest.getLanguagesAllowed()))
                                                   .contestType(modelUpdateContest.getContestType())
                                                   .contestShowTag(modelUpdateContest.getContestShowTag())
                                                   .contestShowComment(modelUpdateContest.getContestShowComment())
                                                   .contestPublic(modelUpdateContest.getContestPublic())
                                                   .allowParticipantPinSubmission(modelUpdateContest.getAllowParticipantPinSubmission())
                                                   .canEditCoefficientPoint(modelUpdateContest.getCanEditCoefficientPoint())
                                                   .build();
        return contestService.updateContestWithCache(contestEntity);

    }

    @Override
    public ContestProblem saveProblemInfoInContest(
        ModelProblemInfoInContest modelProblemInfoInContest,
        String userName
    ) {
        String contestId = modelProblemInfoInContest.getContestId();
        String problemId = modelProblemInfoInContest.getProblemId();

//        ContestEntity contest = contestService.findContest(contestId);
//        List<ProblemEntity> problems = contest.getProblems();

//        if (problems.stream().anyMatch(p -> p.getProblemId().equals(problemId)))
//            return null;

        // ProblemEntity newProblem = problemService.findProblem(problemId);
        // problems.add(newProblem);
        // contest.setProblems(problems);

        // contestService.saveContest(contest);

        ContestProblem contestProblem = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);

        if (contestProblem == null) {
            contestProblem = new ContestProblem();
            contestProblem.setContestId(contestId);
            contestProblem.setProblemId(problemId);
            contestProblem.setForbiddenInstructions(modelProblemInfoInContest.getForbiddenInstructions());
        }

        if (modelProblemInfoInContest.getProblemRename().isEmpty()) {
            contestProblem.setProblemRename(modelProblemInfoInContest.getProblemName());
        } else {
            contestProblem.setProblemRename(modelProblemInfoInContest.getProblemRename());
        }

        if (modelProblemInfoInContest.getProblemRecode().isEmpty()) {
            contestProblem.setProblemRecode("P__" + modelProblemInfoInContest.getProblemName());
        } else {
            contestProblem.setProblemRecode(modelProblemInfoInContest.getProblemRecode());
        }

        contestProblem.setSubmissionMode(modelProblemInfoInContest.getSubmissionMode());
        contestProblem.setForbiddenInstructions(modelProblemInfoInContest.getForbiddenInstructions());
        contestProblem.setCoefficientPoint(modelProblemInfoInContest.getCoefficientPoint());

        return contestProblemRepo.save(contestProblem);
    }

    @Override
    public void removeProblemFromContest(String contestId, String problemId, String userName) {
        ContestProblem contestProblem = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
        if (contestProblem != null) {
            contestProblemRepo.delete(contestProblem);
        }
    }

    @Override
    public ModelGetContestDetailResponse getContestDetailByContestIdAndTeacher(String contestId, String userName) {
        List<UserRegistrationContestEntity> lc = userRegistrationContestRepo
            .findByContestIdAndUserIdAndStatus(
                contestId,
                userName,
                Constants.RegistrationType.SUCCESSFUL.getValue());

        boolean ok = (lc != null && !lc.isEmpty()) || (userName.equals("admin"));

        ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
        if (!ok || contestEntity == null) {
            return ModelGetContestDetailResponse.builder()
                                                .unauthorized(true)
                                                .build();
        }
        return contestService.getModelGetContestDetailResponse(contestEntity);
    }

    /**
     * @param submissionId
     * @param testcaseId
     * @return
     */
    @Override
    public List<SubmissionDetailByTestcaseOM> getSubmissionDetailByTestcase(UUID submissionId, UUID testcaseId) {
        ContestSubmissionEntity submission = contestSubmissionRepo.findById(submissionId).orElse(null);
        ContestEntity contest = null;
        String contestId = "";
        String problemId = "";

        if (submission != null) {
            contest = contestRepo.findContestByContestId(submission.getContestId());
            contestId = submission.getContestId();
            problemId = submission.getProblemId();
        }

        String viewSubmitSolutionOutputMode = "N";
        ContestProblem contestProblem = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
        if (contestProblem != null) {
            if (contestProblem.getSubmissionMode() != null) {
                if (contestProblem.getSubmissionMode().equals(ContestProblem.SUBMISSION_MODE_SOLUTION_OUTPUT)) {
                    viewSubmitSolutionOutputMode = "Y";
                }
            }
        }

        List<UUID> activeTestcaseIds = testCaseRepo
            .findAllActiveTestcaseOfProblem(problemId)
            .stream()
            .map(TestCaseEntity::getTestCaseId)
            .filter(id -> testcaseId == null || testcaseId.compareTo(id) == 0)
            .collect(Collectors.toList());

        List<ContestSubmissionTestCaseEntity> submissionTestcases = contestSubmissionTestCaseEntityRepo.findAllByContestSubmissionId(
            (submissionId));

        Map<UUID, ContestSubmissionTestCaseEntity> mapTestcaseIdToLatestSubmission = getMapTestcaseToLatestSubmission(
            activeTestcaseIds,
            submissionTestcases);

        List<SubmissionDetailByTestcaseOM> result = new ArrayList<>();

        for (ContestSubmissionTestCaseEntity st : mapTestcaseIdToLatestSubmission.values()) {
            TestCaseEntity tc = testCaseRepo.findTestCaseByTestCaseId(st.getTestCaseId());
            SubmissionDetailByTestcaseOM testcaseOM = new SubmissionDetailByTestcaseOM(
//                st.getContestSubmissionTestcaseId(),
//                st.getContestId(),
//                st.getProblemId(),
//                st.getSubmittedByUserLoginId(),
                st.getTestCaseId(),
                null,
                st.getStatus(),
                st.getPoint(),
                st.getUsedToGrade(),
                st.getRuntime(),
                st.getMemoryUsage(),
                null,
                null,
                null,
                st.getCreatedStamp(),
                viewSubmitSolutionOutputMode
            );

            if (testcaseId != null) {
                String testcaseContent = "";
                String testcaseOutput = "";
                String participantSolutionOutput = "";
                String stderr = null;

                if (contest != null && tc != null) {
                    testcaseContent = tc.getTestCase();
                    testcaseOutput = tc.getCorrectAnswer();
                    participantSolutionOutput = st.getParticipantSolutionOutput();
                    stderr = st.getStderr();
                }

                testcaseOM.setTestCase(testcaseContent);
                testcaseOM.setTestCaseAnswer(testcaseOutput);
                testcaseOM.setParticipantAnswer(participantSolutionOutput);
                testcaseOM.setStderr(stderr);
            }

            result.add(testcaseOM);
        }

        return result;
    }

    @Override
    public ContestSubmissionEntity teacherDisableSubmission(String userId, UUID submissionId) {
        ContestSubmissionEntity sub = contestSubmissionRepo.findById(submissionId).orElse(null);
        if (sub != null) {
            sub.setManagementStatus(ContestSubmissionEntity.MANAGEMENT_STATUS_DISABLED);
            sub.setLastUpdatedByUserId(userId);
            sub.setUpdateAt(new Date());
            sub = contestSubmissionRepo.save(sub);
            return sub;
        }
        return null;
    }

    @Override
    public ContestSubmissionEntity teacherEnableSubmission(String userId, UUID submissionId) {
        ContestSubmissionEntity sub = contestSubmissionRepo.findById(submissionId).orElse(null);
        if (sub != null) {
            sub.setManagementStatus(ContestSubmissionEntity.MANAGEMENT_STATUS_ENABLED);
            sub.setLastUpdatedByUserId(userId);
            sub.setUpdateAt(new Date());
            sub = contestSubmissionRepo.save(sub);
            return sub;
        }
        return null;
    }

    private Map<UUID, ContestSubmissionTestCaseEntity> getMapTestcaseToLatestSubmission(
        List<UUID> activeTestcaseIds,
        List<ContestSubmissionTestCaseEntity> submissionTestcases
    ) {

        submissionTestcases = submissionTestcases.stream()
                                                 .filter(testcase -> activeTestcaseIds.contains(testcase.getTestCaseId()))
                                                 .collect(Collectors.toList());

        Map<UUID, ContestSubmissionTestCaseEntity> mapTestcaseIdToLatestSubmission = new HashMap<>();
        for (ContestSubmissionTestCaseEntity submissionTestCase : submissionTestcases) {
            UUID testCaseId = submissionTestCase.getTestCaseId();
            if (!mapTestcaseIdToLatestSubmission.containsKey(testCaseId)) {
                mapTestcaseIdToLatestSubmission.put(testCaseId, submissionTestCase);
            } else {
                ContestSubmissionTestCaseEntity oldSubmissionTestcase = mapTestcaseIdToLatestSubmission.get(testCaseId);
                if (submissionTestCase.getLastUpdatedStamp().after(oldSubmissionTestcase.getLastUpdatedStamp())) {
                    mapTestcaseIdToLatestSubmission.put(testCaseId, submissionTestCase);
                }
            }
        }

        return mapTestcaseIdToLatestSubmission;
    }

    /**
     * @param userId
     * @param submissionId
     * @return
     */
    @Override
    public List<SubmissionDetailByTestcaseOM> getParticipantSubmissionDetailByTestCase(
        String userId, UUID submissionId
    ) {
        ContestSubmissionEntity submission = contestSubmissionRepo.findContestSubmissionEntityByContestSubmissionId(
            submissionId);

        if (!userId.equals(submission.getUserId())) {
            throw new AccessDeniedException("No permission");
        }

        ContestEntity contest;
        String contestId = "";
        String problemId = "";

        contest = contestRepo.findContestByContestId(submission.getContestId());
        contestId = submission.getContestId();
        problemId = submission.getProblemId();

        String viewSubmitSolutionOutputMode = "N";
        ContestProblem contestProblem = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
        if (contestProblem != null) {
            if (contestProblem.getSubmissionMode() != null) {
                if (contestProblem.getSubmissionMode().equals(ContestProblem.SUBMISSION_MODE_SOLUTION_OUTPUT)) {
                    viewSubmitSolutionOutputMode = "Y";
                }
            }
        }

        List<UUID> activeTestcaseIds = testCaseRepo
            .findAllActiveTestcaseOfProblem(problemId)
            .stream()
            .map(TestCaseEntity::getTestCaseId)
            .collect(Collectors.toList());

        List<ContestSubmissionTestCaseEntity> submissionTestcases = contestSubmissionTestCaseEntityRepo.findAllByContestSubmissionId(
            (submissionId));

        Map<UUID, ContestSubmissionTestCaseEntity> mapTestcaseIdToLatestSubmission = getMapTestcaseToLatestSubmission(
            activeTestcaseIds,
            submissionTestcases);

        List<SubmissionDetailByTestcaseOM> result = new ArrayList<>();
        for (ContestSubmissionTestCaseEntity st : mapTestcaseIdToLatestSubmission.values()) {
            String testCaseContent = "";
            String testCaseOutput = "";
            String participantSolutionOutput = "";

            TestCaseEntity tc = testCaseRepo.findTestCaseByTestCaseId(st.getTestCaseId());
            if (contest != null && tc != null) {
                switch (contest.getParticipantViewResultMode()) {
                    case ContestEntity.CONTEST_PARTICIPANT_VIEW_TESTCASE_DETAIL_ENABLED:
                        testCaseContent = tc.getTestCase();
                        testCaseOutput = tc.getCorrectAnswer();
                        participantSolutionOutput = st.getParticipantSolutionOutput();
                        break;

                    case ContestEntity.CONTEST_PARTICIPANT_VIEW_TESTCASE_DETAIL_DISABLED:
                        if (tc.getIsPublic().equals("Y")) {
                            testCaseContent = tc.getTestCase();
                            testCaseOutput = tc.getCorrectAnswer();
                            participantSolutionOutput = st.getParticipantSolutionOutput();

                        } else {
                            testCaseContent = "---HIDDEN---";
                            testCaseOutput = "---HIDDEN---";
                            participantSolutionOutput = "---HIDDEN---";
                        }
                        break;
                    case ContestEntity.CONTEST_PARTICIPANT_VIEW_TESTCASE_DETAIL_INPUT_PARTICIPANT_OUTPUT:
                        testCaseContent = tc.getTestCase();
                        testCaseOutput = "---HIDDEN---";
                        if (tc.getIsPublic().equals("Y")) {
                            testCaseOutput = tc.getCorrectAnswer();
                        }
                        participantSolutionOutput = st.getParticipantSolutionOutput();
                        break;
                }
                //String graded = ContestSubmissionTestCaseEntity.USED_TO_GRADE_YES;

                result.add(new SubmissionDetailByTestcaseOM(
//                    st.getContestSubmissionTestcaseId(),
//                    st.getContestId(),
//                    st.getProblemId(),
//                    st.getSubmittedByUserLoginId(),
                    st.getTestCaseId(),
                    testCaseContent,
                    st.getStatus(),
                    st.getPoint(),
                    st.getUsedToGrade(),
                    st.getRuntime(),
                    st.getMemoryUsage(),
                    testCaseOutput,
                    participantSolutionOutput,
                    null,
                    st.getCreatedStamp(),
                    viewSubmitSolutionOutputMode
                ));
            }
        }

        return result;
    }

    @Override
    public void sendSubmissionToQueue(ContestSubmissionEntity submission) {
        int languageId = -1;
        switch (ComputerLanguage.Languages.valueOf(submission.getSourceCodeLanguage())) {
            case C:
                languageId = 50;
                break;
            case CPP11:
                languageId = 54;
                break;
            case CPP14:
                languageId = 54;
                break;
            case CPP:
            case CPP17:
                languageId = 54;
                break;
            case JAVA:
                languageId = 62;
                break;
            case PYTHON3:
                languageId = 71;
                break;
        }

        String routingKey;
        if (judge0Utils.isMultiThreadedProgram(languageId, submission.getSourceCode())) {
            routingKey = MULTI_THREADED_PROGRAM;
        } else {
            routingKey = JUDGE_PROBLEM;
        }

        rabbitTemplate.convertAndSend(
            EXCHANGE,
            routingKey,
            submission.getContestSubmissionId()
        );
    }

//    @Transactional
//    @Override
//    public ModelContestSubmissionResponse submitSolutionOutput(
//        String solutionOutput,
//        String contestId,
//        String problemId,
//        UUID testCaseId,
//        String userName
//    ) throws Exception {
//        ProblemEntity problemEntity = problemRepo.findByProblemId(problemId);
//
//        UserRegistrationContestEntity userRegistrationContest = null;
//        List<UserRegistrationContestEntity> userRegistrationContests = userRegistrationContestRepo.findUserRegistrationContestEntityByContestIdAndUserIdAndStatus(
//            contestId, userName, Constants.RegistrationType.SUCCESSFUL.getValue());
//        if (userRegistrationContests != null && userRegistrationContests.size() > 0) {
//            userRegistrationContest = userRegistrationContests.get(0);
//        }
//
//        //  log.info("submitSolutionOutput, userRegistrationContest {}", userRegistrationContest);
//        if (userRegistrationContest == null) {
//            throw new MiniLeetCodeException("User not register contest");
//        }
//        TestCaseEntity testCase = testCaseRepo.findTestCaseByTestCaseId(testCaseId);
//        Judge0Submission response = judgeSubmissionTestCaseOutput(
//            problemEntity.getSolutionCheckerSourceCode(),
//            problemEntity.getSolutionCheckerSourceLanguage(),
//            solutionOutput,
//            testCase,
//            1000000,
//            problemEntity.getMemoryLimit());
//
//
//        //  log.info("submitSolutionOutput, response = " + response);
//
//        ProblemSubmission problemSubmission = StringHandler.handleContestResponseSubmitSolutionOutputOneTestCase(
//            response,
//            testCase.getTestCasePoint());
//
//        String participantAns = "";
//        if (problemSubmission.getParticipantAns() != null && !problemSubmission.getParticipantAns().isEmpty()) {
//            participantAns = problemSubmission.getParticipantAns().get(0);
//        }
//        ContestSubmissionTestCaseEntity cste = ContestSubmissionTestCaseEntity.builder()
//                                                                              .contestId(contestId)
//                                                                              .problemId(problemId)
//                                                                              .testCaseId(testCase.getTestCaseId())
//                                                                              .submittedByUserLoginId(userName)
//                                                                              .point(problemSubmission.getScore())
//                                                                              .status(problemSubmission.getStatus())
//                                                                              .participantSolutionOutput(participantAns)
//                                                                              .runtime(problemSubmission.getRuntime())
//                                                                              .createdStamp(new Date())
//                                                                              .build();
//        cste = contestSubmissionTestCaseEntityRepo.save(cste);
//
//        return ModelContestSubmissionResponse.builder()
//                                             .status(problemSubmission.getStatus())
//                                             .testCasePass("1/1")
//                                             .runtime(problemSubmission.getRuntime())
//                                             .memoryUsage((float) 0.0)
//                                             .problemName(problemEntity.getProblemName())
//                                             .contestSubmissionID(null)
//                                             .submittedAt(new Date())
//                                             .score((long) problemSubmission.getScore())
//                                             .build();
//
//    }
//
//    @Override
//    public ModelContestSubmissionResponse submitSolutionOutputOfATestCase(
//        String userId,
//        String solutionOutput,
//        ModelSubmitSolutionOutputOfATestCase m
//    ) {
//        ModelContestSubmissionResponse res = new ModelContestSubmissionResponse();
//        ContestSubmissionEntity sub = contestSubmissionRepo.findContestSubmissionEntityByContestSubmissionId(m.getSubmissionId());
//        if (sub == null) {
//            return res;
//        }
//        String contestId = sub.getContestId();
//        String problemId = sub.getProblemId();
//        ProblemEntity problemEntity = problemRepo.findByProblemId(problemId);
//
//        UserRegistrationContestEntity userRegistrationContest = null;
//        List<UserRegistrationContestEntity> userRegistrationContests = userRegistrationContestRepo.findUserRegistrationContestEntityByContestIdAndUserIdAndStatus(
//            contestId, userId, Constants.RegistrationType.SUCCESSFUL.getValue());
//        if (userRegistrationContests != null && userRegistrationContests.size() > 0) {
//            userRegistrationContest = userRegistrationContests.get(0);
//        }
//
//        //log.info("submitSolutionOutput, userRegistrationContest {}", userRegistrationContest);
//        if (userRegistrationContest == null) {
//            return res;
//        }
//        TestCaseEntity testCase = testCaseRepo.findTestCaseByTestCaseId(m.getTestCaseId());
//        try {
//            Judge0Submission response = judgeSubmissionTestCaseOutput(
//                problemEntity.getSolutionCheckerSourceCode(),
//                problemEntity.getSolutionCheckerSourceLanguage(),
//                solutionOutput,
//                testCase,
//                1000000,
//                problemEntity.getMemoryLimit());
//
//            //   log.info("submitSolutionOutput, response = " + response);
//            ProblemSubmission problemSubmission = StringHandler.handleContestResponseSubmitSolutionOutputOneTestCase(
//                response,
//                testCase.getTestCasePoint());
//
//            String participantAns = "";
//            if (problemSubmission.getParticipantAns() != null && !problemSubmission.getParticipantAns().isEmpty()) {
//                participantAns = problemSubmission.getParticipantAns().get(0);
//            }
//            ContestSubmissionTestCaseEntity cste = null;
//            List<ContestSubmissionTestCaseEntity> l_cste = contestSubmissionTestCaseEntityRepo
//                .findAllByContestSubmissionIdAndTestCaseId(sub.getContestSubmissionId(), m.getTestCaseId());
//            long subPoint = sub.getPoint();
//            if (l_cste != null && !l_cste.isEmpty()) {
//                cste = l_cste.get(0);
//                subPoint = subPoint - cste.getPoint();// reduce point of submission by old point of test-case
//                cste.setPoint(problemSubmission.getScore());
//                cste.setStatus(problemSubmission.getStatus());
//                cste.setParticipantSolutionOutput(solutionOutput);
//                cste.setRuntime(problemSubmission.getRuntime());
//                cste.setCreatedStamp(new Date());
//            } else {
//                cste = ContestSubmissionTestCaseEntity.builder()
//                                                      .contestId(contestId)
//                                                      .problemId(problemId)
//                                                      .contestSubmissionId(sub.getContestSubmissionId())
//                                                      .testCaseId(testCase.getTestCaseId())
//                                                      .submittedByUserLoginId(userId)
//                                                      .point(problemSubmission.getScore())
//                                                      .status(problemSubmission.getStatus())
//                                                      .participantSolutionOutput(participantAns)
//                                                      .runtime(problemSubmission.getRuntime())
//                                                      .createdStamp(new Date())
//                                                      .build();
//            }
//
//            cste = contestSubmissionTestCaseEntityRepo.save(cste);
//
//            subPoint = subPoint + cste.getPoint(); // update Point;
//            sub.setPoint(subPoint);
//            sub.setStatus(ContestSubmissionEntity.SUBMISSION_STATUS_PARTIAL);
//            sub = contestSubmissionRepo.save(sub);
//
//            return ModelContestSubmissionResponse.builder()
//                                                 .contestId(contestId)
//                                                 .problemId(problemId)
//                                                 .contestSubmissionID(sub.getContestSubmissionId())
//                                                 .selectedTestCaseId(m.getTestCaseId())
//                                                 .status(problemSubmission.getStatus())
//                                                 .testCasePass("1/1")
//                                                 .runtime(problemSubmission.getRuntime())
//                                                 .memoryUsage((float) 0.0)
//                                                 .problemName(problemEntity.getProblemName())
//                                                 .submittedAt(new Date())
//                                                 .score((long) problemSubmission.getScore())
//                                                 .build();
//
//        } catch (Exception e) {
//            return res;
//        }
//
//    }

    @Override
    public ModelStudentRegisterContestResponse studentRegisterContest(
        String contestId,
        String userId
    ) throws MiniLeetCodeException {
        ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
        UserRegistrationContestEntity existed = userRegistrationContestRepo.findUserRegistrationContestEntityByContestIdAndUserIdAndRoleId(
            contestId,
            userId,
            UserRegistrationContestEntity.ROLE_PARTICIPANT);

        if (existed == null) {
            UserRegistrationContestEntity userRegistrationContestEntity = UserRegistrationContestEntity
                .builder()
                .contestId(contestId)
                .userId(userId)
                .status(Constants.RegistrationType.PENDING.getValue())
                .roleId(UserRegistrationContestEntity.ROLE_PARTICIPANT)
                .createdStamp(new Date())
                .permissionId(UserRegistrationContestEntity.PERMISSION_SUBMIT)
                .build();
            userRegistrationContestRepo.save(userRegistrationContestEntity);

        } else {
            if (Constants.RegistrationType.SUCCESSFUL.getValue().equals(existed.getStatus())) {
                throw new MiniLeetCodeException("You are already register course successful");
            } else {
                existed.setStatus(Constants.RegistrationType.PENDING.getValue());
                userRegistrationContestRepo.save(existed);
            }
        }
        notificationsService.create(
            userId,
            contestEntity.getUserId(),
            userId + " register contest " + contestId,
            "/programming-contest/contest-manager/" + contestId + "#pending");

        return ModelStudentRegisterContestResponse.builder()
                                                  .status(Constants.RegistrationType.PENDING.getValue())
                                                  .message("You have send request to register contest " +
                                                           contestId +
                                                           ", please wait to accept")
                                                  .build();
    }

    @Override
    public int teacherManageStudentRegisterContest(
        String teacherId,
        ModelTeacherManageStudentRegisterContest modelTeacherManageStudentRegisterContest
    ) throws MiniLeetCodeException {
        ContestEntity contestEntity = contestRepo.findContestByContestId(modelTeacherManageStudentRegisterContest.getContestId());
        int cnt = 0;
        if (contestEntity.getUserId() == null || !contestEntity.getUserId().equals(teacherId)) {
            throw new MiniLeetCodeException(teacherId +
                                            " does not have privilege to manage contest " +
                                            modelTeacherManageStudentRegisterContest.getContestId());
        }
        List<UserRegistrationContestEntity> userRegistrationContestEntities = userRegistrationContestRepo.findUserRegistrationContestEntityByContestIdAndUserId(
            modelTeacherManageStudentRegisterContest.getContestId(),
            modelTeacherManageStudentRegisterContest.getUserId());

        UserRegistrationContestEntity userRegistrationContestEntity = null;
        if (userRegistrationContestEntities != null && userRegistrationContestEntities.size() > 0) {
            userRegistrationContestEntity = userRegistrationContestEntities.get(0);
        }

        if (Constants.RegisterCourseStatus.SUCCESSES
            .getValue()
            .equals(modelTeacherManageStudentRegisterContest.getStatus())) {
            if (!userRegistrationContestEntity.getStatus().equals(Constants.RegistrationType.SUCCESSFUL.getValue())) {
                userRegistrationContestEntity.setStatus(Constants.RegistrationType.SUCCESSFUL.getValue());
                userRegistrationContestRepo.save(userRegistrationContestEntity);
                notificationsService.create(
                    teacherId,
                    modelTeacherManageStudentRegisterContest.getUserId(),
                    "Your register contest " +
                    modelTeacherManageStudentRegisterContest.getContestId() +
                    " is approved ",
                    null);
                cnt += 1;
            }
        } else if (Constants.RegisterCourseStatus.FAILED
            .getValue()
            .equals(modelTeacherManageStudentRegisterContest.getStatus())) {

            if (!userRegistrationContestEntity.getStatus().equals(Constants.RegistrationType.FAILED.getValue())) {
                userRegistrationContestEntity.setStatus(Constants.RegistrationType.FAILED.getValue());
                userRegistrationContestRepo.save(userRegistrationContestEntity);
                notificationsService.create(
                    teacherId,
                    modelTeacherManageStudentRegisterContest.getUserId(),
                    "Your register contest " +
                    modelTeacherManageStudentRegisterContest.getContestId() +
                    " is rejected ",
                    null);
                cnt += 1;
            }
        } else {
            throw new MiniLeetCodeException("Status not found");
        }
        return cnt;
    }

    @Override
    public boolean approveRegisteredUser2Contest(
        String teacherId,
        ModelApproveRegisterUser2ContestInput input
    ) {

        UserRegistrationContestEntity u = userRegistrationContestRepo.findById(input.getId()).orElse(null);
        if (u != null) {
            u.setStatus(UserRegistrationContestEntity.STATUS_SUCCESSFUL);
            u.setLastUpdated(new Date());
            u.setUpdatedByUserLogin_id(teacherId);
            u = userRegistrationContestRepo.save(u);
            return true;
        }

        return false;
    }

    @Override
    public ModelGetContestPageResponse getAllContestsPagingByAdmin(String userName, Pageable pageable) {
        Page<ContestEntity> contestEntities = contestPagingAndSortingRepo.findAll(pageable);
        long count = contestPagingAndSortingRepo.count();
        return getModelGetContestPageResponse(contestEntities, count);
    }

    @Override
    public List<ModelGetContestResponse> getManagedContestOfTeacher(String userName) {
        List<String> roles = new ArrayList<>();
        roles.add(UserRegistrationContestEntity.ROLE_OWNER);
        roles.add(UserRegistrationContestEntity.ROLE_MANAGER);
        List<UserRegistrationContestEntity> userRegistrationContestList = userRegistrationContestRepo.findAllByUserIdAndRoleIdIn(
            userName,
            roles);

        Map<String, List<String>> mapContestIdToRoleList = new HashMap<>();
        for (UserRegistrationContestEntity userRegistrationContest : userRegistrationContestList) {
            String contestId = userRegistrationContest.getContestId();
            String role = userRegistrationContest.getRoleId();
            mapContestIdToRoleList.computeIfAbsent(contestId, k -> new ArrayList<>())
                                  .add(role);
        }

        Map<String, String> mapContestIdToRoleListString = new HashMap<>();
        mapContestIdToRoleList.forEach((contestId, roleList) -> {
            String rolesString = String.join(", ", roles);
            mapContestIdToRoleListString.put(contestId, rolesString);
        });

        Set<String> contestIds = mapContestIdToRoleListString.keySet();
        List<ContestEntity> contests = contestRepo.findByContestIdInAndStatusIdNot(
            contestIds,
            ContestEntity.CONTEST_STATUS_DISABLED);
        List<ModelGetContestResponse> res = contests.stream()
                                                    .map(contest -> ModelGetContestResponse.builder()
                                                                                           .contestId(contest.getContestId())
                                                                                           .contestName(contest.getContestName())
                                                                                           .startAt(contest.getStartedAt())
                                                                                           .statusId(contest.getStatusId())
                                                                                           .userId(contest.getUserId())
                                                                                           .roleId(
                                                                                               mapContestIdToRoleListString.get(
                                                                                                   contest.getContestId()))
                                                                                           .build())
                                                    .collect(Collectors.toList());

        res.sort((a, b) -> {
            if (a.getStartAt() == null && b.getStartAt() == null) {
                return 0;
            } else if (a.getStartAt() == null) {
                return 1;
            } else if (b.getStartAt() == null) {
                return -1;
            } else {
                return b.getStartAt().compareTo(a.getStartAt());
            }
        });

        return res;
    }

    @Override
    public List<ModelGetContestResponse> getAllContests(String userName) {
        /*
        List<String> roles = new ArrayList<>();
        roles.add(UserRegistrationContestEntity.ROLE_OWNER);
        roles.add(UserRegistrationContestEntity.ROLE_MANAGER);
        List<UserRegistrationContestEntity> userRegistrationContestList = userRegistrationContestRepo.findAllByUserIdAndRoleIdIn(userName, roles);

        Map<String, List<String>> mapContestIdToRoleList = new HashMap<>();
        for (UserRegistrationContestEntity userRegistrationContest : userRegistrationContestList) {
            String contestId = userRegistrationContest.getContestId();
            String role = userRegistrationContest.getRoleId();
            mapContestIdToRoleList.computeIfAbsent(contestId, k -> new ArrayList<>())
                                  .add(role);
        }

        Map<String, String> mapContestIdToRoleListString = new HashMap<>();
        mapContestIdToRoleList.forEach((contestId, roleList) -> {
            String rolesString = String.join(", ", roles);
            mapContestIdToRoleListString.put(contestId, rolesString);
        });
        */

        List<ModelGetContestResponse> res = new ArrayList<>();
        List<ContestEntity> allContests = contestRepo.findAll();
        for (ContestEntity contest : allContests) {
            ModelGetContestResponse modelGetContestResponse = ModelGetContestResponse.builder()
                                                                                     .contestId(contest.getContestId())
                                                                                     .contestName(contest.getContestName())
                                                                                     .startAt(contest.getStartedAt())
                                                                                     .statusId(contest.getStatusId())
                                                                                     .userId(contest.getUserId())
                                                                                     .roleId("")
                                                                                     .build();
            res.add(modelGetContestResponse);
        }
        /*
        for (Map.Entry<String, String> e : mapContestIdToRoleListString.entrySet()) {
            ContestEntity contest = contestRepo.findContestByContestId(e.getKey());
            if(contest.getStatusId().equals(ContestEntity.CONTEST_STATUS_DISABLED)){
                continue;
            }
            ModelGetContestResponse modelGetContestResponse = ModelGetContestResponse.builder()
                                                                                     .contestId(contest.getContestId())
                                                                                     .contestName(contest.getContestName())
                                                                                     .startAt(contest.getStartedAt())
                                                                                     .statusId(contest.getStatusId())
                                                                                     .userId(contest.getUserId())
                                                                                     .roleId(e.getValue())
                                                                                     .build();
            res.add(modelGetContestResponse);
        }
        */
        res.sort((a, b) -> {
            if (a.getStartAt() == null && b.getStartAt() == null) {
                return 0;
            } else if (a.getStartAt() == null) {
                return 1;
            } else if (b.getStartAt() == null) {
                return -1;
            } else {
                return b.getStartAt().compareTo(a.getStartAt());
            }
        });
        return res;
    }

    @Override
    public ListModelUserRegisteredContestInfo getListUserRegisterContestSuccessfulPaging(
        Pageable pageable,
        String contestId
    ) {
        Page<ModelUserRegisteredClassInfo> list = userRegistrationContestPagingAndSortingRepo.getAllUserRegisteredByContestIdAndStatusInfo(
            pageable,
            contestId,
            Constants.RegistrationType.SUCCESSFUL.getValue());
        return ListModelUserRegisteredContestInfo.builder()
                                                 .contents(list)
                                                 .build();
    }

    @Override
    public List<ContestMembers> getListMemberOfContest(String contestId) {
////       BUG: This implementation meet N+1 problem
//        List<UserRegistrationContestEntity> lst = userRegistrationContestRepo.findAllByContestIdAndStatus(
//            contestId,
//            UserRegistrationContestEntity.STATUS_SUCCESSFUL);
//
//        List<ModelMemberOfContestResponse> res = new ArrayList<>();
//        for (UserRegistrationContestEntity u : lst) {
//            ModelMemberOfContestResponse m = new ModelMemberOfContestResponse();
//            m.setId(u.getId());
//            m.setContestId(contestId);
//            m.setUserId(u.getUserId());
//            m.setRoleId(u.getRoleId());
//            m.setFullName(userService.getUserFullName(u.getUserId()));
//            m.setLastUpdatedDate(u.getLastUpdated());
//            m.setUpdatedByUserId(u.getUpdatedByUserLogin_id());
//            m.setPermissionId(u.getPermissionId());
//            res.add(m);
//        }
//
//        return res;

        return userRegistrationContestRepo.findByContestIdAndStatus(
            contestId,
            UserRegistrationContestEntity.STATUS_SUCCESSFUL);
    }

    @Override
    public List<ModelMemberOfContestResponse> getListMemberOfContestGroup(String contestId, String userId) {
        List<UserRegistrationContestEntity> lst = userRegistrationContestRepo.findAllInGroupByContestIdAndStatus(
            contestId, userId,
            UserRegistrationContestEntity.STATUS_SUCCESSFUL);
        List<ModelMemberOfContestResponse> res = new ArrayList<>();
        for (UserRegistrationContestEntity u : lst) {
            ModelMemberOfContestResponse m = new ModelMemberOfContestResponse();
            m.setId(u.getId());
            m.setContestId(contestId);
            m.setUserId(u.getUserId());
            m.setRoleId(u.getRoleId());
            m.setFullName(userService.getUserFullName(u.getUserId()));
            m.setLastUpdatedDate(u.getLastUpdated());
            m.setUpdatedByUserId(u.getUpdatedByUserLogin_id());
            m.setPermissionId(u.getPermissionId());
            res.add(m);
        }
        return res;
    }

    @Override
    public ListModelUserRegisteredContestInfo getListUserRegisterContestPendingPaging(
        Pageable pageable,
        String contestId
    ) {
        Page<ModelUserRegisteredClassInfo> list = userRegistrationContestPagingAndSortingRepo.getAllUserRegisteredByContestIdAndStatusInfo(
            pageable,
            contestId,
            Constants.RegistrationType.PENDING.getValue());
        return ListModelUserRegisteredContestInfo.builder()
                                                 .contents(list)
                                                 .build();
    }

    @Override
    public List<ModelMemberOfContestResponse> getPendingRegisteredUsersOfContest(String contestId) {
        List<UserRegistrationContestEntity> lst = userRegistrationContestRepo.findAllByContestIdAndStatus(
            contestId,
            UserRegistrationContestEntity.STATUS_PENDING);
        List<ModelMemberOfContestResponse> res = new ArrayList<>();
        for (UserRegistrationContestEntity u : lst) {
            ModelMemberOfContestResponse m = new ModelMemberOfContestResponse();
            m.setId(u.getId());
            m.setContestId(contestId);
            m.setUserId(u.getUserId());
            m.setRoleId(u.getRoleId());
            m.setFullName(userService.getUserFullName(u.getUserId()));
            res.add(m);
        }
        return res;
    }

    @Override
    public ModelGetContestPageResponse getRegisteredContestsForParticipant(String userId) {
        List<ContestEntity> contests = userRegistrationContestRepo
            .findRegisteredContestsForParticipant(
                userId,
                UserRegistrationContestEntity.ROLE_PARTICIPANT,
                UserRegistrationContestEntity.STATUS_SUCCESSFUL);

        List<ModelGetContestResponse> res = contests.stream()
                                                    .map(contest -> ModelGetContestResponse.builder()
                                                                                           .contestId(contest.getContestId())
                                                                                           .contestName(contest.getContestName())
                                                                                           .contestTime(contest.getContestSolvingTime())
                                                                                           .countDown(contest.getCountDown())
                                                                                           .startAt(contest.getStartedAt())
                                                                                           .statusId(contest.getStatusId())
                                                                                           .userId(contest.getUserId())
                                                                                           .createdAt(contest.getCreatedAt())
                                                                                           .build())
                                                    .collect(Collectors.toList());

        return ModelGetContestPageResponse.builder()
                                          .contests(res)
                                          .build();
    }

    @Override
    public ModelGetContestPageResponse getNotRegisteredContestByUser(Pageable pageable, String userName) {
        Page<ContestEntity> list = userRegistrationContestPagingAndSortingRepo.getNotRegisteredContestByUserLogin(
            pageable,
            userName);
        long count = userRegistrationContestPagingAndSortingRepo.getNumberOfNotRegisteredContestByUserLogin(userName);
        return getModelGetContestPageResponse(list, count);
    }

    @Override
    public List<ContestSubmissionsByUser> getRankingByContestIdNew(
        String contestId,
        Constants.GetPointForRankingType getPointForRankingType
    ) {
        List<String> userIds = userRegistrationContestRepo
            .getAllUserIdsInContest(contestId, Constants.RegistrationType.SUCCESSFUL.getValue())
            .stream()
            .distinct()
            .collect(Collectors.toList());

        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        List<String> problemIds = new ArrayList<>();

        List<ContestProblem> contestProblems = contestProblemRepo.findAllByContestId(contestId);
        LinkedHashMap<String, Double> mapProblemIdToCoefficient = new LinkedHashMap<>();
        if (contestProblems != null) {
            for (ContestProblem cp : contestProblems) {
                if (cp.getSubmissionMode() != null) {
                    if (!cp.getSubmissionMode().equals(ContestProblem.SUBMISSION_MODE_HIDDEN)) {
                        problemIds.add(cp.getProblemId());

                        double coefficient = 1.0;
                        if (contest.getCanEditCoefficientPoint() != null
                            && Integer.valueOf(1).equals(contest.getCanEditCoefficientPoint())) {
                            coefficient = cp.getCoefficientPoint() != null ? cp.getCoefficientPoint() : 1.0;
                        }

                        mapProblemIdToCoefficient.put(cp.getProblemId(), coefficient);
                    }
                }
            }
        }


        /*
        List<String> problemIds = contestRepo
            .findContestByContestId(contestId)
            .getProblems()
            .stream()
            .map(ProblemEntity::getProblemId)
            .collect(Collectors.toList());
        */
        LinkedHashMap<String, String> mapProblemIdToProblemName = new LinkedHashMap<>();
        for (ContestProblem contestProblem : contestProblems) {
            mapProblemIdToProblemName.put(contestProblem.getProblemId(), contestProblem.getProblemRename());
        }

        int nbProblems = problemIds.size();

        HashMap<String, Long> mProblem2MaxPoint = new HashMap<>();
        for (String problemId : problemIds) {
            long totalPoint = 0;
            List<TestCaseEntity> TC = testCaseRepo.findAllByProblemId(problemId);
            for (TestCaseEntity tc : TC) {
                if ("Y".equals(contest.getEvaluateBothPublicPrivateTestcase())) {
                    totalPoint += tc.getTestCasePoint();
                } else {
                    if (tc.getIsPublic().equals("N")) {
                        totalPoint += tc.getTestCasePoint();
                    }
                }
            }
            mProblem2MaxPoint.put(problemId, totalPoint);
        }

        List<ContestSubmissionsByUser> listContestSubmissionsByUser = new ArrayList<>();
        for (String userId : userIds) {
            ContestSubmissionsByUser contestSubmission = new ContestSubmissionsByUser();
            contestSubmission.setUserId(userId);

            LinkedHashMap<String, Long> mapProblemToPoint = new LinkedHashMap<>();
            HashMap<String, Double> mapProblem2PointPercentage = new HashMap<>();

            for (String problemId : problemIds) {
                mapProblemToPoint.put(problemId, 0L);
            }

            List<ModelSubmissionInfoRanking> submissionsByUser = new ArrayList<>();

            boolean allowPinSubmission = contest != null &&
                                         Integer.valueOf(1).equals(contest.getAllowParticipantPinSubmission());

            switch (getPointForRankingType) {
                case HIGHEST:
                    if (allowPinSubmission) {
                        submissionsByUser = contestSubmissionRepo.getHighestPinnedSubmissions(userId, contestId);
                        if (submissionsByUser.isEmpty()) {
                            submissionsByUser = contestSubmissionRepo.getHighestSubmissions(userId, contestId);
                        }
                    } else {
                        submissionsByUser = contestSubmissionRepo.getHighestSubmissions(userId, contestId);
                    }
                    break;
                case LATEST:
                    if (allowPinSubmission) {
                        submissionsByUser = contestSubmissionRepo.getLatestPinnedSubmissions(userId, contestId);
                        if (submissionsByUser.isEmpty()) {
                            submissionsByUser = contestSubmissionRepo.getLatestSubmissions(userId, contestId);
                        }
                    } else {
                        submissionsByUser = contestSubmissionRepo.getLatestSubmissions(userId, contestId);
                    }
                    break;
            }
            //log.info("getRankingByContestIdNew, submisionByUser.sz = " + submissionsByUser.size());

            for (ModelSubmissionInfoRanking submission : submissionsByUser) {
                //log.info("getRankingByContestIdNew, submisionByUser, point = " + submission.getPoint());
                String problemId = submission.getProblemId();
                if (mapProblemToPoint.containsKey(problemId)) {
                    mapProblemToPoint.put(problemId, submission.getPoint());
                }
                long problemPoint = 0;
                if (mProblem2MaxPoint.get(problemId) != null) {
                    problemPoint = mProblem2MaxPoint.get(problemId);
                }
                double percentage = 0;
                if (problemPoint > 0) {
                    percentage = submission.getPoint() * 1.0 / problemPoint;
                    System.out.println("RANKING, problem " +
                                       problemId +
                                       " problemPoint = " +
                                       problemPoint +
                                       " submission points = " +
                                       submission.getPoint());
                }
                mapProblem2PointPercentage.put(problemId, percentage);
            }

            double totalWeightedPoint = 0;
            double totalWeightedPercent = 0;
            double totalCoefficient = 0;

            List<ModelSubmissionInfoRanking> mapProblemsToPoints = new ArrayList<>();
            for (Map.Entry entry : mapProblemToPoint.entrySet()) {
                ModelSubmissionInfoRanking tmp = new ModelSubmissionInfoRanking();
                String problemId = entry.getKey().toString();
                tmp.setProblemId(mapProblemIdToProblemName.get(problemId));
                long point = (Long) entry.getValue();
                tmp.setPoint(point);

                double coefficient = mapProblemIdToCoefficient.getOrDefault(problemId, 1.0);
                long maxPoint = mProblem2MaxPoint.getOrDefault(problemId, 0L);
                double percent = 0;
                if (maxPoint > 0) {
                    percent = (double) point / maxPoint;
                }

                totalWeightedPoint += point * coefficient;
                totalWeightedPercent += percent * coefficient;
                totalCoefficient += coefficient;

                tmp.setPointPercentage(percent);
                mapProblemsToPoints.add(tmp);
            }

            double totalPoint = 0;
            double totalPercentage = 0;

            if (totalCoefficient > 0) {
                totalPoint = totalWeightedPoint / totalCoefficient;
                totalPercentage = totalWeightedPercent / totalCoefficient;
            }

            //contestSubmission.setFullname(userService.getUserFullName(userId));
            contestSubmission.setFullname(getUserFullNameOfContest(contestId, userId));
            contestSubmission.setMapProblemsToPoints(mapProblemsToPoints);
            contestSubmission.setTotalPoint(Double.parseDouble(String.format("%.2f", totalPoint)));
            contestSubmission.setTotalPercentagePoint(totalPercentage);
            contestSubmission.setStringTotalPercentagePoint(String.format("%,.2f", totalPercentage * 100) + "%");

            listContestSubmissionsByUser.add(contestSubmission);

        }

        return listContestSubmissionsByUser;
    }

    @Override
    public List<ContestSubmissionsByUser> getRankingGroupByContestIdNew(
        String userId,
        String contestId,
        Constants.GetPointForRankingType getPointForRankingType
    ) {

        List<ContestSubmissionsByUser> listContestSubmissionsByUser = getRankingByContestIdNew(
            contestId,
            getPointForRankingType);
        List<ContestSubmissionsByUser> selectedlistContestSubmissionsByUser = new ArrayList<>();
        List<ContestUserParticipantGroup> contestUserParticipantGroups = contestUserParticipantGroupRepo
            .findAllByContestIdAndUserId(contestId, userId);
        HashSet<String> participantIds = new HashSet<>();
        for (ContestUserParticipantGroup e : contestUserParticipantGroups) {
            participantIds.add(e.getParticipantId());
        }
        for (ContestSubmissionsByUser e : listContestSubmissionsByUser) {
            if (participantIds.contains(e.getUserId())) {
                selectedlistContestSubmissionsByUser.add(e);
            }
        }
        return selectedlistContestSubmissionsByUser;
    }

//    @Override
//    public Page<ProblemEntity> getPublicProblemPaging(Pageable pageable) {
//        return problemPagingAndSortingRepo.findAllByPublicIs(pageable);
//    }

    @Override
    public Page<ModelGetTestCaseDetail> getTestCaseByProblem(String problemId, TestCaseFilter filter) {
        Pageable pageable = CommonUtils.getPageable(
            filter.getPage(),
            filter.getSize(),
            Sort.by("lastUpdatedStamp").descending());

        if (filter.getFullView() != null && filter.getFullView()) {
            return testCaseRepo.getFullByProblemId(problemId, filter.getPublicOnly(), pageable);
        } else {
            return testCaseRepo.getPreviewByProblemId(problemId, pageable);
        }
    }

    @Override
    public TestCaseDetailProjection getTestCaseDetail(UUID testCaseId) {
        TestCaseDetailProjection testCase = testCaseRepo.getTestCaseDetailByTestCaseId(
            testCaseId,
            1_048_576); // 1MB, align with reality
        if (testCase == null) {
            throw new EntityNotFoundException("Test case not found");
        } else {
            return testCase;
        }
    }

//    @Override
//    public void editTestCase(UUID testCaseId, ModelSaveTestcase modelSaveTestcase) throws MiniLeetCodeException {
//        TestCaseEntity testCase = testCaseRepo.findTestCaseByTestCaseId(testCaseId);
//        if (testCase == null) {
//            throw new MiniLeetCodeException("test case not found");
//        }
//
//        testCase.setTestCase(modelSaveTestcase.getInput());
//        testCase.setCorrectAnswer(modelSaveTestcase.getResult());
//        testCase.setTestCasePoint(modelSaveTestcase.getPoint());
//        testCase.setIsPublic(modelSaveTestcase.getIsPublic());
//        testCaseService.saveTestCaseWithCache(testCase);
//    }

    @Override
    public ModelAddUserToContestResponse addUserToContest(ModelAddUserToContest modelAddUserToContest) {
        String contestId = modelAddUserToContest.getContestId();
        String userId = modelAddUserToContest.getUserId();
        String role = modelAddUserToContest.getRole();

        ModelAddUserToContestResponse response = new ModelAddUserToContestResponse();
        response.setUserId(userId);
        response.setRoleId(role);

        if (userLoginRepo.findByUserLoginId(userId) == null) {
            response.setStatus("User not found");
            return response;
        }

        UserRegistrationContestEntity userRegistrationContest = userRegistrationContestRepo
            .findUserRegistrationContestEntityByContestIdAndUserIdAndRoleId(contestId, userId, role);


        if (userRegistrationContest != null &&
            userRegistrationContest.getStatus().equals(Constants.RegistrationType.SUCCESSFUL.getValue())) {
            response.setStatus("Added");
            return response;
        }


        if (userRegistrationContest == null) {
            userRegistrationContestRepo.save(UserRegistrationContestEntity.builder()
                                                                          .contestId(modelAddUserToContest.getContestId())
                                                                          .userId(modelAddUserToContest.getUserId())
                                                                          .status(Constants.RegistrationType.SUCCESSFUL.getValue())
                                                                          .roleId(modelAddUserToContest.getRole())
                                                                          .fullname(modelAddUserToContest.getFullname())
                                                                          .permissionId(UserRegistrationContestEntity.PERMISSION_SUBMIT)
                                                                          .build());
        } else {
            userRegistrationContest.setStatus(Constants.RegistrationType.SUCCESSFUL.getValue());
            userRegistrationContestRepo.save(userRegistrationContest);
        }
        response.setStatus("Successful");
        return response;
    }

    @Override
    public ModelAddUserToContestResponse updateUserFullnameOfContest(ModelAddUserToContest modelAddUserToContest) {
        String contestId = modelAddUserToContest.getContestId();
        String userId = modelAddUserToContest.getUserId();
        String role = modelAddUserToContest.getRole();
        String fullname = modelAddUserToContest.getFullname();

        ModelAddUserToContestResponse response = new ModelAddUserToContestResponse();
        response.setUserId(userId);
        response.setRoleId(role);
        response.setFullname(fullname);

        if (userLoginRepo.findByUserLoginId(userId) == null) {
            response.setStatus("User not found");
            return response;
        }

        List<UserRegistrationContestEntity> userRegistrationContests = userRegistrationContestRepo
            .findUserRegistrationContestEntityByContestIdAndUserId(contestId, userId);
        if (userRegistrationContests != null) {
            for (UserRegistrationContestEntity u : userRegistrationContests) {
                u.setFullname(modelAddUserToContest.getFullname());
                u = userRegistrationContestRepo.save(u);
            }
        }

        response.setStatus("Successful");
        return response;

    }

    @Transactional
    @Override
    public void addUsers2ToContest(String contestId, AddUsers2Contest addUsers2Contest) {
        List<String> userIds = addUsers2Contest.getUserIds() != null
            ? addUsers2Contest.getUserIds()
            : new ArrayList<>();

        List<String> groupUserIds = new ArrayList<>();
        if (addUsers2Contest.getGroupIds() != null && !addUsers2Contest.getGroupIds().isEmpty()) {
            groupUserIds = teacherGroupRelationRepository.findUserIdsByGroupIds(addUsers2Contest.getGroupIds());
        }

        Set<String> allUserIds = new HashSet<>();
        allUserIds.addAll(userIds);
        allUserIds.addAll(groupUserIds);

        for (String userId : allUserIds) {
            ModelAddUserToContest model = new ModelAddUserToContest(
                contestId,
                userId,
                addUsers2Contest.getRole(),
                ""
            );
            addUserToContest(model);
        }
    }


    @Override
    public ModelAddUserToContestGroupResponse addUserToContestGroup(ModelAddUserToContestGroup modelAddUserToContestGroup) {
        String contestId = modelAddUserToContestGroup.getContestId();
        String userId = modelAddUserToContestGroup.getUserId();
        String participantId = modelAddUserToContestGroup.getParticipantId();

        ModelAddUserToContestGroupResponse response = new ModelAddUserToContestGroupResponse();
        response.setUserId(userId);
        response.setParticipantId(participantId);

        if (userLoginRepo.findByUserLoginId(participantId) == null) {
            response.setStatus("User not found");
            return response;
        }

        ContestUserParticipantGroup cupg = contestUserParticipantGroupRepo
            .findByContestIdAndUserIdAndParticipantId(contestId, userId, participantId);

        if (cupg != null) {
            response.setStatus("Added");
            return response;
        }


        contestUserParticipantGroupRepo.save(ContestUserParticipantGroup.builder()
                                                                        .contestId(contestId)
                                                                        .userId(userId)
                                                                        .participantId(participantId)
                                                                        .build());
        response.setStatus("Successful");

        return response;
    }

    @Override
    public void deleteUserContest(ModelAddUserToContest modelAddUserToContest) throws MiniLeetCodeException {
        //UserRegistrationContestEntity userRegistrationContest = userRegistrationContestRepo.findUserRegistrationContestEntityByContestIdAndUserId(
        //    modelAddUserToContest.getContestId(),
        //    modelAddUserToContest.getUserId());
        UserRegistrationContestEntity userRegistrationContest = userRegistrationContestRepo.findUserRegistrationContestEntityByContestIdAndUserIdAndRoleId(
            modelAddUserToContest.getContestId(),
            modelAddUserToContest.getUserId(), modelAddUserToContest.getRole());

        if (userRegistrationContest == null) {
            throw new MiniLeetCodeException("user not register contest");
        }

        userRegistrationContest.setStatus(Constants.RegistrationType.FAILED.getValue());
        userRegistrationContestRepo.delete(userRegistrationContest);
    }

    @Override
    public Page<ContestSubmission> findContestSubmissionByUserLoginIdPaging(Pageable pageable, String userLoginId) {
        return contestSubmissionPagingAndSortingRepo.findAllByUserId(pageable, userLoginId)
                                                    .map(contestSubmissionEntity -> ContestSubmission
                                                        .builder()
                                                        .contestSubmissionId(contestSubmissionEntity.getContestSubmissionId())
                                                        .contestId(contestSubmissionEntity.getContestId())
                                                        .createAt(contestSubmissionEntity.getCreatedAt() != null
                                                                      ? DateTimeUtils.dateToString(
                                                            contestSubmissionEntity.getCreatedAt(),
                                                            DateTimeUtils.DateTimeFormat.DATE_TIME_ISO_FORMAT)
                                                                      : null)
                                                        .sourceCodeLanguage(contestSubmissionEntity.getSourceCodeLanguage())
                                                        .point(contestSubmissionEntity.getPoint())
                                                        .problemId(contestSubmissionEntity.getProblemId())
                                                        .testCasePass(contestSubmissionEntity.getTestCasePass())
                                                        .status(contestSubmissionEntity.getStatus())
                                                        .message(contestSubmissionEntity.getMessage())
                                                        .userId(contestSubmissionEntity.getUserId())
                                                        .build()
                                                    );
    }

    @Override
    public Page<ContestSubmission> findContestSubmissionByUserLoginIdAndContestIdPaging(
        Pageable pageable,
        String userLoginId,
        String contestId
    ) {
        //log.info("findContestSubmissionByUserLoginIdAndContestIdPaging, user = " + userLoginId + " contestId = " + contestId);

        contestProblemPermissionUtil.checkContestAccess(userLoginId, contestId);
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        if (contest != null && ContestEntity.CONTEST_STATUS_OPEN.equals(contest.getStatusId())) {
            return Page.empty(pageable);
        }
        
        return contestSubmissionPagingAndSortingRepo.findAllByUserIdAndContestId(pageable, userLoginId, contestId)
                                                    .map(contestSubmissionEntity -> ContestSubmission
                                                        .builder()
                                                        .contestSubmissionId(contestSubmissionEntity.getContestSubmissionId())
                                                        .contestId(contestSubmissionEntity.getContestId())
                                                        .createAt(contestSubmissionEntity.getCreatedAt() != null
                                                                      ? DateTimeUtils.dateToString(
                                                            contestSubmissionEntity.getCreatedAt(),
                                                            DateTimeUtils.DateTimeFormat.DATE_TIME_ISO_FORMAT)
                                                                      : null)
                                                        .sourceCodeLanguage(contestSubmissionEntity.getSourceCodeLanguage())
                                                        .point(contestSubmissionEntity.getPoint())
                                                        .problemId(contestSubmissionEntity.getProblemId())
                                                        .testCasePass(contestSubmissionEntity.getTestCasePass())
                                                        .status(contestSubmissionEntity.getStatus())
                                                        .message(contestSubmissionEntity.getMessage())
                                                        .userId(contestSubmissionEntity.getUserId())
                                                        .build()
                                                    );
    }

    @Override
    public Page<ContestSubmission> findContestSubmissionByUserLoginIdAndContestIdAndProblemIdPaging(
        Pageable pageable,
        String userLoginId,
        String contestId,
        String problemId
    ) {
        //log.info("findContestSubmissionByUserLoginIdAndContestIdPaging, user = " + userLoginId + " contestId = " + contestId);

        contestProblemPermissionUtil.checkContestAccess(userLoginId, contestId);
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        if (contest != null && ContestEntity.CONTEST_STATUS_OPEN.equals(contest.getStatusId())) {
            return Page.empty(pageable);
        }
        
        Integer allowParticipantPinSubmission = contest != null ? contest.getAllowParticipantPinSubmission() : 0;
        return contestSubmissionPagingAndSortingRepo
            .findAllByUserIdAndContestIdAndProblemId(pageable, userLoginId, contestId, problemId)
            .map(contestSubmissionEntity -> ContestSubmission
                .builder()
                .contestSubmissionId(contestSubmissionEntity.getContestSubmissionId())
                .contestId(contestSubmissionEntity.getContestId())
                .createAt(contestSubmissionEntity.getCreatedAt() != null
                              ? DateTimeUtils.dateToString(
                    contestSubmissionEntity.getCreatedAt(),
                    DateTimeUtils.DateTimeFormat.DATE_TIME_ISO_FORMAT)
                              : null)
                .sourceCodeLanguage(contestSubmissionEntity.getSourceCodeLanguage())
                .point(contestSubmissionEntity.getPoint())
                .problemId(contestSubmissionEntity.getProblemId())
                .testCasePass(contestSubmissionEntity.getTestCasePass())
                .status(contestSubmissionEntity.getStatus())
                .message(contestSubmissionEntity.getMessage())
                .userId(contestSubmissionEntity.getUserId())
                .finalSelectedSubmission(contestSubmissionEntity.getFinalSelectedSubmission())
                .allowParticipantPinSubmission(allowParticipantPinSubmission)
                .build()
            );
    }

    @Override
    public List<ContestSubmission> getNewestSubmissionResults(String userLoginId) {
        List<ContestSubmissionEntity> lst = contestSubmissionPagingAndSortingRepo
            .findAllByUserId(userLoginId);
        List<ContestSubmission> retList = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (ContestSubmissionEntity s : lst) {
            String k = s.getContestId() + "@" + s.getProblemId() + "@" + s.getUserId();
            keys.add(k);
            //  log.info("getNewestSubmissionResults, read record " + s.getContestSubmissionId() + " created stamp " + s.getCreatedAt());
        }
        Set<String> ignores = new HashSet<>();
        for (ContestSubmissionEntity s : lst) {
            String k = s.getContestId() + "@" + s.getProblemId() + "@" + s.getUserId();
            if (ignores.contains(k)) {
                continue;
            }
            if (keys.contains(k)) {
                ContestSubmission cs = new ContestSubmission();
                cs.setContestSubmissionId(s.getContestSubmissionId());
                cs.setStatus(s.getStatus());
                cs.setContestId(s.getContestId());
                cs.setProblemId(s.getProblemId());
                cs.setUserId(s.getUserId());
                cs.setPoint(s.getPoint());
                cs.setCreateAt(s.getCreatedAt() != null
                                   ? DateTimeUtils.dateToString(
                    s.getCreatedAt(),
                    DateTimeUtils.DateTimeFormat.DATE_TIME_ISO_FORMAT)
                                   : null);
                cs.setTestCasePass(s.getTestCasePass());
                cs.setSourceCodeLanguage(s.getSourceCodeLanguage());
                retList.add(cs);
                ignores.add(k);// process only the first meet
                //break;// break when reach a first entry
            }
        }
        return retList;
    }

    private String getUserFullNameOfContest(String contestId, String userId) {
        String fullname = userRegistrationContestService.findUserFullnameOfContest(contestId, userId);
        if (fullname == null) {
            fullname = userService.getUserFullName(userId);
        }
        return fullname;
    }

    @Override
    public Page<ContestSubmission> findContestSubmissionByContestIdPaging(
        String contestId,
        SubmissionFilter filter
    ) {
        Pageable pageable = CommonUtils.getPageable(
            filter.getPage(),
            filter.getSize(),
            Sort.by("createdAt").descending());

        return contestSubmissionPagingAndSortingRepo
            .searchSubmissionInContestPaging(
                contestId,
                StringUtils.defaultString(filter.getUserId()),
                StringUtils.defaultString(filter.getProblemId()),
                pageable)
            .map(submission -> ContestSubmission
                .builder()
                .contestSubmissionId(submission.getContestSubmissionId())
                .contestId(submission.getContestId())
                .createAt(submission.getCreatedAt() != null
                              ? DateTimeUtils.dateToString(
                    submission.getCreatedAt(),
                    DateTimeUtils.DateTimeFormat.DATE_TIME_ISO_FORMAT)
                              : null)
                .sourceCodeLanguage(submission.getSourceCodeLanguage())
                .point(submission.getPoint())
                .problemId(submission.getProblemId())
                //.problemName(problemService.getProblemName(submission.getProblemId()))
                .problemName(contestService.getProblemNameInContest(
                    submission.getContestId(),
                    submission.getProblemId()))
                .testCasePass(submission.getTestCasePass())
                .status(submission.getStatus())
                .managementStatus(submission.getManagementStatus())
                .violationForbiddenInstruction(submission.getViolateForbiddenInstruction())
                .violationForbiddenInstructionMessage(submission.getViolateForbiddenInstructionMessage())
                .message(submission.getMessage())
                .userId(submission.getUserId())
                //.fullname(userService.getUserFullName(submission.getUserId()))
                //.fullname(userRegistrationContestService.findUserFullnameOfContest(contestId,submission.getUserId()))
                .fullname(getUserFullNameOfContest(contestId, submission.getUserId()))
                .createdByIp(submission.getCreatedByIp())
                .codeAuthorship(submission.getCodeAuthorship())
                .finalSelectedSubmission(submission.getFinalSelectedSubmission())
                .build());
    }

    @Override
    public Page<ContestSubmission> findContestGroupSubmissionByContestIdPaging(
        Pageable pageable, String contestId, String userId,
        String searchTerm
    ) {
        searchTerm = searchTerm.toLowerCase();
        log.info("findContestGroupSubmissionByContestIdPaging, contestId = " + contestId + " userId = " + userId);
        return contestSubmissionPagingAndSortingRepo
//            .findAllByContestId(pageable, contestId)
.searchSubmissionInContestGroupPaging(contestId, userId, searchTerm, searchTerm, pageable)
.map(contestSubmissionEntity -> ContestSubmission
    .builder()
    .contestSubmissionId(contestSubmissionEntity.getContestSubmissionId())
    .contestId(contestSubmissionEntity.getContestId())
    .createAt(contestSubmissionEntity.getCreatedAt() != null
                  ? DateTimeUtils.dateToString(
        contestSubmissionEntity.getCreatedAt(),
        DateTimeUtils.DateTimeFormat.DATE_TIME_ISO_FORMAT)
                  : null)
    .sourceCodeLanguage(contestSubmissionEntity.getSourceCodeLanguage())
    .point(contestSubmissionEntity.getPoint())
    .problemId(contestSubmissionEntity.getProblemId())
    .testCasePass(contestSubmissionEntity.getTestCasePass())
    .status(contestSubmissionEntity.getStatus())
    .message(contestSubmissionEntity.getMessage())
    .userId(contestSubmissionEntity.getUserId())
    .fullname(userService.getUserFullName(contestSubmissionEntity.getUserId()))
    .build());
    }

    @Override
    public ContestSubmissionEntity getContestSubmissionDetailForTeacher(UUID submissionId) {
        return contestSubmissionRepo.findContestSubmissionEntityByContestSubmissionId(submissionId);
    }

    @Override
    public ModelGetContestInfosOfSubmissionOutput getContestInfosOfASubmission(UUID submissionId) {
        ContestSubmissionEntity sub = contestSubmissionRepo.findContestSubmissionEntityByContestSubmissionId(
            submissionId);
        String contestId = sub.getContestId();
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        ModelGetContestInfosOfSubmissionOutput res = new ModelGetContestInfosOfSubmissionOutput();
        res.setSubmissionId(submissionId);
        res.setContestId(contestId);
        List<String> problemIds = new ArrayList();
        for (ProblemEntity p : contest.getProblems()) {
            problemIds.add(p.getProblemId());
        }
        res.setProblemIds(problemIds);
        res.setProblems(contest.getProblems());
        return res;
    }

    @Override
    @Transactional
    public void deleteTestcase(UUID testcaseId, String userId) throws MiniLeetCodeException {
        TestCaseEntity testCase = testCaseRepo.findTestCaseByTestCaseId(testcaseId);
        if (testCase == null) {
            return;
        }

        List<UserContestProblemRole> problemRoles = userContestProblemRoleRepo.findAllByProblemIdAndUserId(
            testCase.getProblemId(),
            userId);

        boolean isAuthorized = problemRoles
            .stream()
            .anyMatch(problemRole -> problemRole.getRoleId().equals(UserContestProblemRole.ROLE_OWNER) ||
                                     problemRole.getRoleId().equals(UserContestProblemRole.ROLE_EDITOR));
        if (!isAuthorized) {
            throw new MiniLeetCodeException("permission denied");
        }

        testCase.setStatusId(TestCaseEntity.STATUS_DISABLED);
        testCaseService.saveTestCaseWithCache(testCase);

    }

    class CodeSimilatiryComparator implements Comparator<CodeSimilarityElement> {

        @Override
        public int compare(CodeSimilarityElement e1, CodeSimilarityElement e2) {
            return Double.compare(e2.getScore(), e1.getScore());
        }
    }

    @Override
    public ModelCodeSimilarityOutput checkSimilarity(String contestId, ModelCheckSimilarityInput I) {
        List<CodeSimilarityElement> list = new ArrayList();

        List<UserRegistrationContestEntity> participants = userRegistrationContestRepo
            .findAllByContestIdAndStatus(contestId, UserRegistrationContestEntity.STATUS_SUCCESSFUL);

        ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
        List<ProblemEntity> problems = contestEntity.getProblems();

        for (ProblemEntity p : problems) {
            String problemId = p.getProblemId();
            //  log.info("checkSimilarity, consider problem " + problemId + " threshold  = " + I.getThreshold());
            List<ContestSubmissionEntity> listSubmissions = new ArrayList();
            for (UserRegistrationContestEntity participant : participants) {
                String userLoginId = participant.getUserId();
                //log.info("checkSimilarity, consider problem " + problemId + " participant " + userLoginId);
                List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestIdAndUserIdAndProblemId(
                    contestId,
                    userLoginId,
                    problemId);
                //log.info("checkSimilarity, consider problem " + problemId + " participant " + userLoginId
                //         + " submissions.sz = " +
                //         submissions.size() +
                //         "");

                if (submissions != null && submissions.size() > 0) {// take the last submission in the sorted list
                    ContestSubmissionEntity sub = submissions.get(0);
                    listSubmissions.add(sub);
                }
            }

            //  log.info("checkSimilarity, consider problem " + problemId + " listSubmissions = " + listSubmissions.size());

            // check similarity of submissions to the current problemId
            for (int i = 0; i < listSubmissions.size(); i++) {
                ContestSubmissionEntity s1 = listSubmissions.get(i);
                for (int j = i + 1; j < listSubmissions.size(); j++) {
                    ContestSubmissionEntity s2 = listSubmissions.get(j);
                    if (s1.getUserId().equals(s2.getUserId())) {
                        continue;
                    }

                    double score = CodeSimilarityCheck.check(s1.getSourceCode(), s2.getSourceCode());
                    //  log.info("checkSimilarity, consider problem " + problemId + " listSubmissions = " + listSubmissions.size()
                    //     + " score between codes " + i + " and " + j + " = " + score + " threshold = " + I.getThreshold());

                    //if(score <= 0.0001) continue;
                    if (score <= I.getThreshold() * 0.01) {
                        continue;
                    }


                    CodeSimilarityElement e = new CodeSimilarityElement();
                    e.setScore(score);
                    e.setSource1(s1.getSourceCode());
                    e.setUserLoginId1(s1.getUserId());
                    e.setSubmitDate1(s1.getCreatedAt());
                    e.setProblemId1(s1.getProblemId());

                    e.setSource2(s2.getSourceCode());
                    e.setUserLoginId2(s2.getUserId());
                    e.setSubmitDate2(s2.getCreatedAt());
                    e.setProblemId2(s2.getProblemId());

                    list.add(e);

                    List<CodePlagiarism> codePlagiarisms = codePlagiarismRepo
                        .findAllByContestIdAndProblemIdAndUserId1AndUserId2(
                            contestId,
                            problemId,
                            s1.getUserId(),
                            s2.getUserId());
                    if (codePlagiarisms != null) {
                        for (CodePlagiarism cp : codePlagiarisms) {
                            cp.setScore(score);
                            cp.setCreatedStamp(new Date());
                            cp = codePlagiarismRepo.save(cp);
                        }
                    } else {

                        CodePlagiarism codePlagiarism = new CodePlagiarism();
                        codePlagiarism.setContestId(contestId);
                        codePlagiarism.setProblemId(problemId);
                        codePlagiarism.setUserId1(s1.getUserId());
                        codePlagiarism.setUserId2(s2.getUserId());
                        codePlagiarism.setSourceCode1(s1.getSourceCode());
                        codePlagiarism.setSourceCode2(s2.getSourceCode());
                        codePlagiarism.setSubmitDate1(s1.getCreatedAt());
                        codePlagiarism.setSubmitDate2(s2.getCreatedAt());
                        codePlagiarism.setScore(score);
                        codePlagiarism.setCreatedStamp(new Date());

                        codePlagiarism = codePlagiarismRepo.save(codePlagiarism);
                        //log.info("checkSimilarity, add new item score = " + score);
                    }
                }
            }
        }

        Collections.sort(list, new CodeSimilatiryComparator());

        ModelCodeSimilarityOutput model = new ModelCodeSimilarityOutput();
        model.setCodeSimilarityElementList(list);
        return model;
    }

    @Override
    public int checkForbiddenInstructions(String contestId) {
        List<ContestProblem> contestProblems = contestProblemRepo.findAllByContestId(contestId);
        Map<String, List<String>> mPro2ForbiddenIns = new HashMap();
        for (ContestProblem cp : contestProblems) {
            // log.info("checkForbiddenInstructions, forbidden instructions = " + cp.getForbiddenInstructions());
            String[] forbiddens = cp.getForbiddenInstructions().split(",");
            if (forbiddens != null) {
                List<String> L = new ArrayList();
                for (String s : forbiddens) {
                    s = s.trim();
                    if (s != null && s != "" && !s.equals("") && s.length() > 0) {
                        L.add(s.trim());
                    }
                }
                mPro2ForbiddenIns.put(cp.getProblemId(), L);
                //log.info("checkForbiddenInstructions, forbidden list L = " + L.toString());
            }
        }
        List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestId(contestId);
        int cnt = 0;
        for (ContestSubmissionEntity sub : submissions) {
            String problemId = sub.getProblemId();
            String msg = "";
            if (mPro2ForbiddenIns.get(problemId) != null) {
                List<String> forbiddenFound = new ArrayList<String>();
                for (String f : mPro2ForbiddenIns.get(problemId)) {
                    //log.info("checkForbiddenInstructions, , sourcecode " + sub.getSourceCode() + " forbidden f = " + f);
                    if (sub.getSourceCode() != null) {
                        if (sub.getSourceCode().contains(f)) {
                            sub.setViolateForbiddenInstruction(ContestSubmissionEntity.VIOLATION_FORBIDDEN_YES);
                            //msg = msg + f + ",";
                            forbiddenFound.add(f);
                            cnt++;
                            //log.info("checkForbiddenInstructions, , sourcecode " + sub.getSourceCode() + " forbidden f = " + f + " DISCOVER violations!!!");

                        }
                    }
                }
                for (int i = 0; i < forbiddenFound.size(); i++) {
                    msg = msg + forbiddenFound.get(i);
                    if (i < forbiddenFound.size() - 1) {
                        msg = msg + " :: ";
                    }
                }
            }
            sub.setViolateForbiddenInstructionMessage(msg);
            contestSubmissionRepo.save(sub);
        }
        return cnt;
    }

    @Override
    public ModelCodeSimilarityOutput computeSimilarity(
        String userLoginId,
        String contestId,
        ModelCheckSimilarityInput I
    ) {
        List<CodeSimilarityElement> list = new ArrayList();
        ModelCodeSimilarityOutput model = new ModelCodeSimilarityOutput();
        List<UserRegistrationContestEntity> participants = userRegistrationContestRepo
            .findAllByContestIdAndStatus(contestId, UserRegistrationContestEntity.STATUS_SUCCESSFUL);

        ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
        List<ProblemEntity> problems = contestEntity.getProblems();

        for (ProblemEntity p : problems) {
            String problemId = p.getProblemId();
            //log.info("computeSimilarity, consider problem " + problemId + " threshold  = " + I.getThreshold());
            List<ContestSubmissionEntity> listSubmissions = new ArrayList();
            for (UserRegistrationContestEntity participant : participants) {
                String userId = participant.getUserId();
                //log.info("computeSimilarity, consider problem " + problemId + " participant " + userId);
                List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestIdAndUserIdAndProblemId(
                    contestId,
                    userId,
                    problemId);
                //log.info("computeSimilarity, consider problem " + problemId + " participant " + userId
                //+ " submissions.sz = " +
                // submissions.size() +
                //  "");

                if (submissions != null && submissions.size() > 0) {// take the last submission in the sorted list
                    for (ContestSubmissionEntity sub : submissions) {
                        //ContestSubmissionEntity sub = submissions.get(0);
                        //if(sub.getPoint() > 0)// consider only submissions having points
                        listSubmissions.add(sub);
                    }
                }
            }
            if (listSubmissions.size() > MAX_SUBMISSIONS_CHECK_SIMILARITY) {
                if (!userLoginId.equals("admin")) {
                    model.setMessage("Too Many submissions, only admin can do this task");
                    return model;
                }
            }
            // SORT listSubmissions in an increasing order of userId
            Collections.sort(
                listSubmissions, new Comparator<ContestSubmissionEntity>() {
                    @Override
                    public int compare(ContestSubmissionEntity o1, ContestSubmissionEntity o2) {
                        return o1.getUserId().compareTo(o2.getUserId());
                    }
                });

            //log.info("computeSimilarity, consider problem " + problemId + " listSubmissions = " + listSubmissions.size());
            //for(ContestSubmissionEntity e: listSubmissions){
            //log.info("computeSimilarity, user " + e.getUserId() + " submissionId " + e.getContestSubmissionId() + " point " + e.getPoint());
            //}

            // check similarity of submissions to the current problemId
            for (int i = 0; i < listSubmissions.size(); i++) {
                ContestSubmissionEntity s1 = listSubmissions.get(i);
                for (int j = i + 1; j < listSubmissions.size(); j++) {
                    ContestSubmissionEntity s2 = listSubmissions.get(j);
                    if (s1.getUserId().equals(s2.getUserId())) {
                        continue;
                    }
                    //log.info("checkSimilarity, consider problem " + problemId + " listSubmissions = " + listSubmissions.size()
                    //         + " score between codes " + i + " length = " + s1.getSourceCode().length() + " " + j + " length = " + s2.getSourceCode().length());

                    double score = CodeSimilarityCheck.check(s1.getSourceCode(), s2.getSourceCode());
                    //log.info("checkSimilarity, consider problem " + problemId + " listSubmissions = " + listSubmissions.size()
                    //     + " score between codes " + i + " and " + j + " = " + score + " threshold = " + I.getThreshold());

                    //if(score <= 0.0001) continue;
                    if (score <= I.getThreshold() * 0.01) {
                        log.info("checkSimilarity, consider problem " +
                                 problemId +
                                 " listSubmissions = " +
                                 listSubmissions.size()
                                 +
                                 " score SMALL between codes " +
                                 i +
                                 " and " +
                                 j +
                                 " = " +
                                 score +
                                 " threshold = " +
                                 I.getThreshold());

                        continue;
                    }
                    log.info("checkSimilarity, consider problem " +
                             problemId +
                             " listSubmissions = " +
                             listSubmissions.size()
                             +
                             " score between codes " +
                             i +
                             " and " +
                             j +
                             " = " +
                             score +
                             " threshold = " +
                             I.getThreshold());


                    CodeSimilarityElement e = new CodeSimilarityElement();
                    e.setScore(score);
                    e.setSource1(s1.getSourceCode());
                    e.setUserLoginId1(s1.getUserId());
                    e.setSubmitDate1(s1.getCreatedAt());
                    e.setProblemId1(s1.getProblemId());

                    e.setSource2(s2.getSourceCode());
                    e.setUserLoginId2(s2.getUserId());
                    e.setSubmitDate2(s2.getCreatedAt());
                    e.setProblemId2(s2.getProblemId());

                    list.add(e);

                    List<CodePlagiarism> codePlagiarisms = codePlagiarismRepo
                        .findAllByContestIdAndProblemIdAndSubmissionId1AndSubmissionId2(
                            contestId, problemId,
                            s1.getContestSubmissionId(),
                            s2.getContestSubmissionId());

                    if (codePlagiarisms != null && codePlagiarisms.size() > 0) {
                        //log.info("checkSimilarity, codePlagiarism sz = " + codePlagiarisms.size());
                        for (CodePlagiarism cp : codePlagiarisms) {
                            cp.setScore(score);
                            cp.setCreatedStamp(new Date());
                            cp = codePlagiarismRepo.save(cp);
                            //log.info("checkSimilarity, codePlagiarism sz = " + codePlagiarisms.size() + " exist -> update score " + score);
                        }
                    } else {

                        CodePlagiarism codePlagiarism = new CodePlagiarism();
                        codePlagiarism.setContestId(contestId);
                        codePlagiarism.setProblemId(problemId);
                        codePlagiarism.setUserId1(s1.getUserId());
                        codePlagiarism.setUserId2(s2.getUserId());
                        codePlagiarism.setSourceCode1(s1.getSourceCode());
                        codePlagiarism.setSourceCode2(s2.getSourceCode());
                        codePlagiarism.setSubmitDate1(s1.getCreatedAt());
                        codePlagiarism.setSubmitDate2(s2.getCreatedAt());
                        codePlagiarism.setSubmissionId1(s1.getContestSubmissionId());
                        codePlagiarism.setSubmissionId2(s2.getContestSubmissionId());
                        codePlagiarism.setScore(score);
                        codePlagiarism.setCreatedStamp(new Date());

                        codePlagiarism = codePlagiarismRepo.save(codePlagiarism);
                        //log.info("computeSimilarity, add new item score = " + score);
                    }
                }
            }
        }

        Collections.sort(list, new CodeSimilatiryComparator());


        model.setCodeSimilarityElementList(list);
        return model;
    }

    @Override
    public void evaluateSubmission(UUID submissionId) {
        ContestSubmissionEntity submission = contestSubmissionRepo.findById(submissionId).orElse(null);
        ContestEntity contest = contestService.findContestWithCache(submission.getContestId());
        evaluateSubmission(submission, contest);
    }

    @Override
    public void evaluateSubmissions(String contestId, String problemId) {
        List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestIdAndProblemId(
            contestId,
            problemId);
        if (submissions == null) {
            log.info("evaluateSubmissions, contest " + contestId + " problem " + problemId + " -> NO Submissions");
            return;
        }
        log.info("evaluateSubmissions, contest " +
                 contestId +
                 " problem " +
                 problemId +
                 " nbSubmissions = " +
                 submissions.size());
        ContestEntity contest = contestService.findContestWithCache(contestId);

        for (ContestSubmissionEntity sub : submissions) {
            log.info("evaluateSubmissions, contest " +
                     contestId +
                     " problem " +
                     problemId +
                     " submission " +
                     sub.getContestSubmissionId());
            evaluateSubmission(sub, contest);
        }

    }

    @Override
    public void evaluateSubmissionUsingQueue(ContestSubmissionEntity submission) {
        contestService.updateContestSubmissionStatus(
            submission.getContestSubmissionId(),
            ContestSubmissionEntity.SUBMISSION_STATUS_EVALUATION_IN_PROGRESS);

        sendSubmissionToQueue(submission);
    }


    @Override
    public void evaluateSubmission(ContestSubmissionEntity sub, ContestEntity contest) {
        if (sub != null) {
            // QUEUE MODE
            if (contest.getJudgeMode().equals(ContestEntity.ASYNCHRONOUS_JUDGE_MODE_QUEUE)) {
                evaluateSubmissionUsingQueue(sub);
            }
        }
    }

    @Override
    public ModelEvaluateBatchSubmissionResponse reJudgeAllSubmissionsOfContest(String contestId) {
        List<UserRegistrationContestEntity> participants = userRegistrationContestRepo
            .findAllByContestIdAndStatus(contestId, UserRegistrationContestEntity.STATUS_SUCCESSFUL);

        ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
        List<ProblemEntity> problems = contestEntity.getProblems();
        for (UserRegistrationContestEntity participant : participants) {
            String userLoginId = participant.getUserId();
            for (ProblemEntity p : problems) {
                String problemId = p.getProblemId();
                List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestIdAndUserIdAndProblemId(
                    contestId,
                    userLoginId,
                    problemId);

                for (ContestSubmissionEntity sub : submissions) {// take the last submission in the sorted list
                    evaluateSubmission(sub, contestEntity);
                }
            }
        }
        return null;

    }

    private ModelGetContestPageResponse getModelGetContestPageResponse(Page<ContestEntity> contestPage, long count) {
        List<ModelGetContestResponse> lists = new ArrayList<>();
        if (contestPage != null) {
            contestPage.forEach(contest -> {
                ModelGetContestResponse modelGetContestResponse = ModelGetContestResponse.builder()
                                                                                         .contestId(contest.getContestId())
                                                                                         .contestName(contest.getContestName())
                                                                                         .contestTime(contest.getContestSolvingTime())
                                                                                         .countDown(contest.getCountDown())
                                                                                         .startAt(contest.getStartedAt())
                                                                                         .statusId(contest.getStatusId())
                                                                                         .userId(contest.getUserId())
                                                                                         .createdAt(contest.getCreatedAt())
                                                                                         .build();
                lists.add(modelGetContestResponse);
            });
        }

        return ModelGetContestPageResponse.builder()
                                          .contests(lists)
                                          .count(count)
                                          .build();
    }

//    /**
//     * FIXME
//     * @param sourceChecker
//     * @param computerLanguage
//     * @param submissionTestCaseOutput
//     * @param testCase
//     * @param timeLimit
//     * @param memoryLimit
//     * @return
//     * @throws Exception
//     */
//    private Judge0Submission judgeSubmissionTestCaseOutput(
//        String sourceChecker,
//        String computerLanguage,
//        String submissionTestCaseOutput,
//        TestCaseEntity testCase,
//        int timeLimit,
//        int memoryLimit
//    ) throws Exception {
//        int languageId;
//        String compilerOptions = null;
//        switch (ComputerLanguage.Languages.valueOf(computerLanguage)) {
//            case C:
//                languageId = 50;
//                compilerOptions = "-std=c17 -w -O2 -lm -fmax-errors=3";
//                break;
//            case CPP11:
//                languageId = 54;
//                compilerOptions = "-std=c++11 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
//                break;
//            case CPP14:
//                languageId = 54;
//                compilerOptions = "-std=c++14 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
//                break;
//            case CPP:
//            case CPP17:
//                languageId = 54;
//                compilerOptions = "-std=c++17 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
//                break;
//            case JAVA:
//                languageId = 62;
//                break;
//            case PYTHON3:
//                languageId = 71;
//                break;
//            default:
//                throw new Exception("Language not supported");
//        }
//
//        Judge0Submission submission = Judge0Submission.builder()
//                                                      .sourceCode(sourceChecker)
//                                                      .languageId(languageId)
//                                                      .compilerOptions(compilerOptions)
//                                                      .commandLineArguments(null)
//                                                      .stdin(String.join(
//                                                          "\n", new String[]{
//                                                              testCase.getTestCase(),
//                                                              testCase.getCorrectAnswer(),
//                                                              submissionTestCaseOutput}))
//                                                      .cpuTimeLimit((float) timeLimit)
//                                                      .cpuExtraTime((float) (timeLimit * 1.0 + 2.0))
//                                                      .wallTimeLimit((float) (timeLimit * 1.0 + 10.0))
//                                                      .memoryLimit((float) memoryLimit * 1024)
//                                                      .stackLimit(judge0Config.getSubmission().getMaxStackLimit())
//                                                      .maxProcessesAndOrThreads(2 + (languageId != 62 ? 0 : judge0Config.getSubmission().getJavaMaxProcessesAndOrThreadsExtra())) // OK, for output checking no need multi-threading, except Java
//                                                      .enablePerProcessAndThreadTimeLimit(false)
//                                                      .enablePerProcessAndThreadMemoryLimit(false)
//                                                      .maxFileSize(judge0Config.getSubmission().getMaxMaxFileSize())
//                                                      .redirectStderrToStdout(false)
//                                                      .enableNetwork(false)
//                                                      .numberOfRuns(1)
//                                                      .build();
//
//        submission = judge0Service.createASubmission(submission, true, true);
//        submission.decodeBase64();
//
//        return submission;
//    }

    /**
     * @param testCaseId
     * @param testcaseContent
     * @param dto
     * @return
     */
    @Override
    public Object editTestcase(
        UUID testCaseId,
        String testcaseContent,
        ModelProgrammingContestUploadTestCase dto
    ) throws Exception {
        TestCaseEntity testCase = testCaseRepo
            .findById(testCaseId)
            .orElseThrow(() -> new EntityNotFoundException("Testcase with ID " + testCaseId + " not found"));
        Judge0Submission output = null;

        if (TestcaseUploadMode.EXECUTE.equals(dto.getUploadMode())) {
            if (StringUtils.isNotBlank(testcaseContent)) {
                ProblemEntity problem = problemRepo.findByProblemId(dto.getProblemId());

                output = runCode(
                    problem.getCorrectSolutionSourceCode(),
                    problem.getCorrectSolutionLanguage(),
                    testcaseContent,
                    problem.getMemoryLimit(),
                    getTimeLimitByLanguage(problem, problem.getCorrectSolutionLanguage()));

                if (output.getStatus().getId() != 3) { // Chay khong thanh cong thi khong luu, tra ket qua luon
                    return Judge0Submission.getSubmissionDetailsAfterExecution(output);
                }

                testCase.setTestCase(testcaseContent);
                testCase.setCorrectAnswer(output.getStdout());
            } else {
                throw new IllegalArgumentException("The file is required when using EXECUTE mode");
            }
        } else { // TestcaseUploadMode.NOT_EXECUTE
            if (StringUtils.isNotBlank(testcaseContent)) {
                testCase.setTestCase(testcaseContent);
            }

            if (StringUtils.isNotBlank(dto.getCorrectAnswer())) {
                testCase.setCorrectAnswer(dto.getCorrectAnswer());
            }
        }

        testCase.setTestCasePoint(dto.getPoint());
        testCase.setIsPublic(dto.getIsPublic() ? "Y" : "N");
        testCase.setDescription(dto.getDescription());

        testCaseService.saveTestCaseWithCache(testCase);
        return Judge0Submission.getSubmissionDetailsAfterExecution(output);
    }

    /**
     * @param sourceCode
     * @param computerLanguage
     * @param input
     * @param memoryLimit
     * @param timeLimit
     * @return
     * @throws Exception
     */
    private Judge0Submission runCode(
        String sourceCode,
        String computerLanguage,
        String input,
        float memoryLimit,
        float timeLimit
    ) throws Exception {
        int languageId;
        String compilerOptions = null;
        switch (ComputerLanguage.Languages.valueOf(computerLanguage)) {
            case C:
                languageId = 50;
                compilerOptions = "-std=c17 -w -O2 -lm -fmax-errors=3";
                break;
            case CPP11:
                languageId = 54;
                compilerOptions = "-std=c++11 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
                break;
            case CPP14:
                languageId = 54;
                compilerOptions = "-std=c++14 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
                break;
            case CPP:
            case CPP17:
                languageId = 54;
                compilerOptions = "-std=c++17 -w -O2 -lm -fmax-errors=3 -march=native -s -Wl,-z,stack-size=268435456";
                break;
            case JAVA:
                languageId = 62; // Consider JAVA_OPTS memory limit but seems not necessary with Judge0
                break;
            case PYTHON3:
                languageId = 71;
                break;
            default:
                throw new Exception("Language not supported");
        }

        Judge0Config.ServerConfig serverConfig = judge0Utils.getServerConfig(languageId, sourceCode);
        Judge0Submission submission = Judge0Submission.builder()
                                                      .sourceCode(sourceCode)
                                                      .languageId(languageId)
                                                      .compilerOptions(compilerOptions)
                                                      .commandLineArguments(null)
                                                      .stdin(input)
//                                                      .expectedOutput(Constants.ProblemResultEvaluationType.CUSTOM
//                                                                          .getValue()
//                                                                          .equals(evaluationType)
//                                                                          ? null
//                                                                          : testCase.getCorrectAnswer())
                                                      .cpuTimeLimit(timeLimit)
                                                      .cpuExtraTime((float) (timeLimit + 2.0))
                                                      .wallTimeLimit((float) (timeLimit + 10.0))
                                                      .memoryLimit(memoryLimit * 1024)
                                                      .stackLimit(serverConfig.getSubmission().getMaxStackLimit())
                                                      .maxProcessesAndOrThreads(judge0Utils.getMaxProcessesAndOrThreads(
                                                          languageId,
                                                          sourceCode))
                                                      .enablePerProcessAndThreadTimeLimit(false)
                                                      .enablePerProcessAndThreadMemoryLimit(false)
                                                      .maxFileSize(serverConfig.getSubmission().getMaxMaxFileSize())
                                                      .redirectStderrToStdout(false)
                                                      .enableNetwork(false)
                                                      .numberOfRuns(1)
                                                      .build();

        submission = judge0Service.createASubmission(serverConfig, submission, true, true);
        submission.decodeBase64();

        return submission;
    }

//    @Override
//    public List<CodePlagiarism> findAllByContestId(String contestId) {
//        return codePlagiarismRepo.findAllByContestId(contestId);
//    }

    private boolean emptyString(String s) {
        return s == null || s.isEmpty();
    }

    @Override
    public List<CodePlagiarism> findAllBy(ModelGetCodeSimilarityParams input) {
        List<CodePlagiarism> codePlagiarisms = codePlagiarismRepo.findAllByContestId(input.getContestId());
        List<CodePlagiarism> res = new ArrayList<>();
        if (!emptyString(input.getProblemId()) && !emptyString(input.getUserId())) {
            for (CodePlagiarism e : codePlagiarisms) {
                if (e.getProblemId().equals(input.getProblemId()) &&
                    (e.getUserId1().equals(input.getUserId()) || e.getUserId2().equals(input.getUserId()))) {
                    res.add(e);
                }
            }
        } else if (!emptyString(input.getProblemId()) && emptyString(input.getUserId())) {
            for (CodePlagiarism e : codePlagiarisms) {
                if (e.getProblemId().equals(input.getProblemId())) {
                    res.add(e);
                }
            }
        } else if (emptyString(input.getProblemId()) && !emptyString(input.getUserId())) {
            for (CodePlagiarism e : codePlagiarisms) {
                if (e.getUserId1().equals(input.getUserId()) || e.getUserId2().equals(input.getUserId())) {
                    res.add(e);
                }
            }
        } else {
            return codePlagiarisms;
        }

        return res;
    }

    class DFS {

        private Set<String> V;
        private Map<String, Set<String>> A;
        private Map<String, Integer> idxCC;
        private int nbCC;
        private List<List<String>> connectedComponents;

        public DFS(Set<String> V, Map<String, Set<String>> A) {
            this.V = V;
            this.A = A;
            /*
            for(String e: V){
                String a = "";
                for(String u: A.get(e)) a = a + u + ", ";
                log.info("DFS, node e = " + e + ": " + a);
            }
            */
        }

        private void Try(String u) {
            idxCC.put(u, nbCC);
            //log.info("DFS.Try(" + u + "), nbCC = " + nbCC + " idxCC.put(" + u + "," + nbCC + ")");
            for (String v : A.get(u)) {
                if (idxCC.get(v) == null) {
                    Try(v);
                }
            }
        }

        public void solve() {
            nbCC = 0;
            idxCC = new HashMap();
            connectedComponents = new ArrayList();
            for (String v : V) {
                if (idxCC.get(v) == null) {
                    nbCC++;
                    Try(v);
                }
            }
            for (int i = 1; i <= nbCC; i++) {
                List<String> cc = new ArrayList<String>();
                for (String e : V) {
                    if (idxCC.get(e) == i) {
                        cc.add(e);
                        //log.info("DFS.solve, cc i   = " + i + " add e = " + e);
                    }
                }
                connectedComponents.add(cc);
            }
        }

        public List<List<String>> getConnectedComponents() {
            return connectedComponents;
        }
    }

    @Override
    public List<ModelSimilarityClusterOutput> computeSimilarityClusters(ModelGetCodeSimilarityParams input) {
        ContestEntity contest = contestRepo.findContestByContestId(input.getContestId());
        List<CodePlagiarism> codePlagiarisms = codePlagiarismRepo.findAllByContestId(input.getContestId());
        double threshold = input.getThreshold() * 0.01;
        List<ModelSimilarityClusterOutput> res = new ArrayList();
        for (ProblemEntity p : contest.getProblems()) {
            // build graph and compute connected component related to the selected problem p
            Map<String, Set<String>> A = new HashMap();
            Set<String> V = new HashSet();
            List<CodePlagiarism> EP = new ArrayList();
            for (CodePlagiarism cp : codePlagiarisms) {
                //log.info("computeSimilarityClusters, problem " + p.getProblemId() + " user1 " + cp.getUserId1() + " user2 = "
                //         + cp.getUserId2() + " score = " + cp.getScore() + " threshold = " + threshold);
                if (cp.getProblemId().equals(p.getProblemId()) && cp.getScore() >= threshold) {
                    EP.add(cp);
                    //log.info("computeSimilarityClusters, problem " + p.getProblemId() + "ADD edge (" + cp.getUserId1() + ","
                    //         + cp.getUserId2() + ")");

                    String u1 = cp.getUserId1();
                    String u2 = cp.getUserId2();
                    V.add(u1);
                    V.add(u2);
                    if (A.get(u1) == null) {
                        A.put(u1, new HashSet());
                    }
                    if (A.get(u2) == null) {
                        A.put(u2, new HashSet());
                    }
                    A.get(u1).add(u2);
                    A.get(u2).add(u1);
                }

            }
            DFS dfs = new DFS(V, A);
            dfs.solve();
            List<List<String>> connectedComponents = dfs.getConnectedComponents();
            //ModelSimilarityClusterOutput c = new ModelSimilarityClusterOutput();
            //c.setProblemId(p.getProblemId());
            //c.setClusters(connectedComponents);
            for (List<String> cc : connectedComponents) {
                ModelSimilarityClusterOutput c = new ModelSimilarityClusterOutput();
                c.setProblemId(p.getProblemId());
                StringBuilder userIds = new StringBuilder();
                for (String s : cc) {
                    userIds.append(s).append(", ");
                }
                c.setUserIds(userIds.toString());
                res.add(c);
            }
        }

        return res;
    }

    @Override
    public List<ModelReponseCodeSimilaritySummaryParticipant> getListModelReponseCodeSimilaritySummaryParticipant(String contestId) {
        List<CodePlagiarism> L = codePlagiarismRepo.findAllByContestId(contestId);
        List<ModelReponseCodeSimilaritySummaryParticipant> res = new ArrayList();
        List<UserRegistrationContestEntity> UR = userRegistrationContestRepo.findAllByContestIdAndStatus(
            contestId,
            UserRegistrationContestEntity.STATUS_SUCCESSFUL);
        HashMap<String, Double> mUser2HighestSimilarity = new HashMap();
        for (UserRegistrationContestEntity ur : UR) {
            mUser2HighestSimilarity.put(ur.getUserId(), 0.0);
        }
        for (CodePlagiarism cp : L) {
            String u1 = cp.getUserId1();
            String u2 = cp.getUserId2();
            double s = cp.getScore();
            if (mUser2HighestSimilarity.get(u1) != null && mUser2HighestSimilarity.get(u1) < s) {
                mUser2HighestSimilarity.put(u1, s);
            }
            if (mUser2HighestSimilarity.get(u2) != null && mUser2HighestSimilarity.get(u2) < s) {
                mUser2HighestSimilarity.put(u2, s);
            }
        }
        for (String u : mUser2HighestSimilarity.keySet()) {
            ModelReponseCodeSimilaritySummaryParticipant e = new ModelReponseCodeSimilaritySummaryParticipant();
            e.setUserId(u);
            e.setHighestSimilarity(mUser2HighestSimilarity.get(u));
            res.add(e);
        }
        Collections.sort(
            res, new Comparator<ModelReponseCodeSimilaritySummaryParticipant>() {
                @Override
                public int compare(
                    ModelReponseCodeSimilaritySummaryParticipant u1,
                    ModelReponseCodeSimilaritySummaryParticipant u2
                ) {
                    if (u2.getHighestSimilarity() > u1.getHighestSimilarity()) {
                        return 1;
                    } else if (u2.getHighestSimilarity() < u1.getHighestSimilarity()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
        return res;
    }

    @Transactional
    @Override
    public ContestSubmissionEntity updateContestSubmissionSourceCode(ModelUpdateContestSubmission input) {
        ContestSubmissionEntity sub = contestSubmissionRepo.findById(input.getContestSubmissionId()).orElse(null);
        if (sub != null) {
            sub.setSourceCode(input.getModifiedSourceCodeSubmitted());
            if (input.getProblemId() != null && !input.getProblemId().equals("")) {
                sub.setProblemId(input.getProblemId());
            }
            if (input.getContestId() != null && !input.getContestId().equals("")) {
                sub.setContestId(input.getContestId());
            }

            sub.setUpdateAt(new Date());
            sub = contestSubmissionRepo.save(sub);

            ContestSubmissionHistoryEntity e = new ContestSubmissionHistoryEntity();
            e.setContestSubmissionId(sub.getContestSubmissionId());
            e.setModifiedSourceCodeSubmitted(input.getModifiedSourceCodeSubmitted());
            e.setLanguage(sub.getSourceCodeLanguage());
            e.setProblemId(sub.getProblemId());

            if (input.getContestId() != null && !input.getContestId().equals("")) {
                e.setContestId(sub.getContestId());
            }
            e.setCreatedStamp(new Date());
            e = contestSubmissionHistoryRepo.save(e);
            return sub;
        }
        return null;
    }

    @Override
    public List<ModelGetContestResponse> getContestsUsingAProblem(String problemId) {
        List<ModelGetContestResponse> res = new ArrayList();
        List<ContestProblem> contestProblems = contestProblemRepo.findAllByProblemId(problemId);
        for (ContestProblem cp : contestProblems) {
            ContestEntity contest = contestRepo.findContestByContestId(cp.getContestId());
            ModelGetContestResponse m = ModelGetContestResponse.builder()
                                                               .contestId(contest.getContestId())
                                                               .userId(contest.getUserId())
                                                               .createdAt(contest.getCreatedAt())
                                                               .statusId(contest.getStatusId())
                                                               .build();
            res.add(m);
        }
        return res;
    }

    /**
     * Try to execute the solution code on this test case, if pass then store in DB
     * otherwise, ignore
     *
     * @param testCase
     * @param dto
     * @return
     */
    @Override
    public Object addTestcase(
        String testCase,
        ModelProgrammingContestUploadTestCase dto
    ) throws Exception {
        ProblemEntity problem = problemRepo.findByProblemId(dto.getProblemId());
        String testcaseCorrectAnswer;
        Judge0Submission output = null;

        if (TestcaseUploadMode.EXECUTE.equals(dto.getUploadMode())) {
            output = runCode(
                problem.getCorrectSolutionSourceCode(),
                problem.getCorrectSolutionLanguage(),
                testCase,
                problem.getMemoryLimit(),
                getTimeLimitByLanguage(problem, problem.getCorrectSolutionLanguage()));

            if (output.getStatus().getId() != 3) { // Chay khong thanh cong thi khong luu, tra ket qua luon
                return Judge0Submission.getSubmissionDetailsAfterExecution(output);
            }

            testcaseCorrectAnswer = output.getStdout();
        } else { // TestcaseUploadMode.NOT_EXECUTE
            testcaseCorrectAnswer = dto.getCorrectAnswer();
        }

        TestCaseEntity tc = TestCaseEntity.builder()
                                          .testCase(testCase)
                                          .problemId(dto.getProblemId())
                                          .isPublic(dto.getIsPublic() ? "Y" : "N")
                                          .testCasePoint(dto.getPoint())
                                          .correctAnswer(testcaseCorrectAnswer)
                                          .description(dto.getDescription())
                                          .build();

        testCaseService.saveTestCaseWithCache(tc);
        return Judge0Submission.getSubmissionDetailsAfterExecution(output);
    }

    /**
     * Can be use when editing the solution's source code
     *
     * @param problemId
     * @param testCaseId
     * @return
     */
    @Override
    public Object reCreateTestcaseCorrectAnswer(String problemId, UUID testCaseId) throws Exception {
        ProblemEntity problem = problemRepo
            .findById(problemId)
            .orElseThrow(() -> new EntityNotFoundException("Problem with ID " + problemId + " not found"));
        TestCaseEntity testCase = testCaseRepo
            .findById(testCaseId)
            .orElseThrow(() -> new EntityNotFoundException("Testcase with ID " + testCaseId + " not found"));
        ;

        String testcaseContent = testCase.getTestCase();
        Judge0Submission output = runCode(
            problem.getCorrectSolutionSourceCode(),
            problem.getCorrectSolutionLanguage(),
            testcaseContent,
            problem.getMemoryLimit(),
            getTimeLimitByLanguage(problem, problem.getCorrectSolutionLanguage()));

        if (output.getStatus().getId() != 3) { // Chay khong thanh cong thi khong luu, tra ket qua luon
            return Judge0Submission.getSubmissionDetailsAfterExecution(output);
        }

        testCase.setCorrectAnswer(output.getStdout());
        testCaseService.saveTestCaseWithCache(testCase);

        return Judge0Submission.getSubmissionDetailsAfterExecution(output);
    }

    private void updateMaxPoint(
        ContestSubmissionEntity s,
        HashMap<String, List<ModelUserJudgedProblemSubmissionResponse>> mUserId2Submission,
        HashMap<String, ProblemEntity> mID2Problem
    ) {
        if (mUserId2Submission.get(s.getUserId()) == null) {
            mUserId2Submission.put(s.getUserId(), new ArrayList<>());
            ModelUserJudgedProblemSubmissionResponse e = new ModelUserJudgedProblemSubmissionResponse();
            e.setUserId(s.getUserId());
            e.setFullName(userService.getUserFullName(s.getUserId()));
            e.setProblemId(s.getProblemId());
            e.setSubmissionSourceCode(s.getSourceCode());
            e.setPoint(s.getPoint());
            if (mID2Problem.get(s.getProblemId()) != null) {
                e.setProblemName(mID2Problem.get(s.getProblemId()).getProblemName());
            } else {
                e.setProblemName(s.getProblemId());
            }

            e.setTestCasePassed(s.getTestCasePass());
            e.setStatus(s.getStatus());
            mUserId2Submission.get(s.getUserId()).add(e);
        } else {
            // scan list problem & submission and update max point
            ModelUserJudgedProblemSubmissionResponse maxP = null;
            long maxPoint = -1000;
            for (ModelUserJudgedProblemSubmissionResponse e : mUserId2Submission.get(s.getUserId())) {
                if (e.getProblemId().equals(s.getProblemId())) {
                    if (e.getPoint() > maxPoint) {
                        maxP = e;
                        maxPoint = e.getPoint();
                    }
                }
            }
            if (maxP == null) {
                ModelUserJudgedProblemSubmissionResponse e = new ModelUserJudgedProblemSubmissionResponse();
                e.setUserId(s.getUserId());
                e.setFullName(userService.getUserFullName(s.getUserId()));
                e.setProblemId(s.getProblemId());
                e.setSubmissionSourceCode(s.getSourceCode());
                e.setPoint(s.getPoint());
                if (mID2Problem.get(s.getProblemId()) != null) {
                    e.setProblemName(mID2Problem.get(s.getProblemId()).getProblemName());
                } else {
                    e.setProblemName(s.getProblemId());
                }
                e.setTestCasePassed(s.getTestCasePass());
                e.setStatus(s.getStatus());
                mUserId2Submission.get(s.getUserId()).add(e);
            } else {
                if (maxP.getPoint() < s.getPoint()) {// update max point submission
                    maxP.setPoint(s.getPoint());
                    maxP.setSubmissionSourceCode(s.getSourceCode());
                    maxP.setStatus(s.getStatus());
                    maxP.setTestCasePassed(s.getTestCasePass());
                }
            }
        }
    }

    @Override
    public byte[] getUserJudgedProblemSubmissions(String contestId) {
        List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestId(contestId);
        List<ModelUserJudgedProblemSubmissionResponse> dtos = new ArrayList<>();
        HashMap<String, List<ModelUserJudgedProblemSubmissionResponse>> mUserId2Submission = new HashMap<>();
        HashMap<String, ProblemEntity> mID2Problem = new HashMap<>();
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        List<ProblemEntity> problems = contest.getProblems();
        for (ProblemEntity p : problems) {
            mID2Problem.put(p.getProblemId(), p);
        }
        for (ContestSubmissionEntity s : submissions) {
            updateMaxPoint(s, mUserId2Submission, mID2Problem);
        }
        for (String userId : mUserId2Submission.keySet()) {
            if (mUserId2Submission.get(userId) != null) {
                dtos.addAll(mUserId2Submission.get(userId));
            }
        }

        return exportPdf(dtos, "reports/submission_report.jasper", new HashMap<>());
    }

    @Override
    public ModelGetRolesOfUserInContestResponse getRolesOfUserInContest(String userId, String contestId) {
        List<UserRegistrationContestEntity> lst = userRegistrationContestRepo
            .findByContestIdAndUserIdAndStatus(
                contestId,
                userId,
                UserRegistrationContestEntity.STATUS_SUCCESSFUL);
        List<String> roles = UserRegistrationContestEntity.getListRoles();
        List<String> rolesApproved = new ArrayList();
        List<String> rolesNotApproved = new ArrayList();
        //  log.info("getRolesOfUserInContest, userId = " + userId + " contestId = " + contestId + " lst.sz = " + lst.size());
        for (String role : roles) {
            boolean approved = false;
            for (UserRegistrationContestEntity e : lst) {
                String r = e.getRoleId();
                //  log.info("getRolesOfUserInContest, userId = " + userId + " contestId = " + contestId + " lst.sz = " + lst.size()
                // +" role approved r = " + r + " consider role = " + role);
                if (role.equals(r)) {
                    approved = true;
                    break;
                }
            }
            if (approved) {
                rolesApproved.add(role);
            } else {
                rolesNotApproved.add(role);
            }
        }
        return new ModelGetRolesOfUserInContestResponse(userId, contestId, rolesApproved, rolesNotApproved);
    }

    @Override
    public boolean removeMemberFromContest(UUID id) {
        UserRegistrationContestEntity u = userRegistrationContestRepo.findById(id).orElse(null);
        if (u != null) {
            ContestEntity contest = contestRepo.findContestByContestId(u.getContestId());
            String createdBy = "";
            if (contest != null) {
                createdBy = contest.getUserId();
            }
            if (u.getUserId().equals("admin")) {
                return false;
            }
            if (u.getUserId().equals(createdBy)) {
                return false;
            }
            userRegistrationContestRepo.delete(u);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeMemberFromContestGroup(String contestId, String userId, String participantId) {
        ContestUserParticipantGroup item = contestUserParticipantGroupRepo
            .findByContestIdAndUserIdAndParticipantId(contestId, userId, participantId);
        if (item == null) {

            log.info("removeMemberFromContestGroup, cannot find record for contest " +
                     contestId +
                     " user " +
                     userId +
                     " participant " +
                     participantId);
            return false;
        } else {
            contestUserParticipantGroupRepo.delete(item);
            log.info("removeMemberFromContestGroup, DELETED record for contest " +
                     contestId +
                     " user " +
                     userId +
                     " participant " +
                     participantId);

            return true;
        }
    }

    @Override
    public boolean updatePermissionMemberToContest(String userId, ModelUpdatePermissionMemberToContestInput input) {
        UserRegistrationContestEntity u = userRegistrationContestRepo.findById(input.getUserRegisId()).orElse(null);
        if (u != null) {
            u.setPermissionId(input.getPermissionId());
            u.setLastUpdated(new Date());
            u.setUpdatedByUserLogin_id(userId);
            userRegistrationContestRepo.save(u);
            return true;
        }
        return false;
    }

    @Override
    public List<TagEntity> getAllTags() {
        return tagRepo.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    @Transactional
    public TagEntity addNewTag(ModelTag tag) {
        TagEntity tagEntity = new TagEntity();
        tagEntity.setName(tag.getName());

        if (tag.getDescription() != null) {
            tagEntity.setDescription(tag.getDescription());
        } else {
            tagEntity.setDescription("");
        }

        return tagRepo.save(tagEntity);
    }

//    @Override
//    @Transactional
//    public TagEntity updateTag(Integer tagId, ModelTag newTag) {
//        TagEntity tagEntity = tagRepo.findByTagId(tagId);
//
//        tagEntity.setName(newTag.getName());
//
//        if (newTag.getDescription() != null) {
//            tagEntity.setDescription(newTag.getDescription());
//        } else {
//            tagEntity.setDescription("");
//        }
//
//        return tagRepo.save(tagEntity);
//    }
//
//    @Override
//    @Transactional
//    public void deleteTag(Integer tagId) {
//        TagEntity tagEntity = tagRepo.findByTagId(tagId);
//        tagRepo.delete(tagEntity);
//    }

    @Override
    @Transactional
    public void switchAllContestJudgeMode(String judgeMode) {
        contestRepo.switchAllContestToJudgeMode(judgeMode);
        cacheService.flushAllCache();
    }

    @Override
    public List<ContestProblemModelResponse> extApiGetAllProblems(String userID) {
        List<ProblemEntity> problems = problemRepo.findAll();
        List<ContestProblemModelResponse> res = new ArrayList<>();
        for (ProblemEntity pe : problems) {
            ContestProblemModelResponse p = new ContestProblemModelResponse(
                pe.getProblemId(), pe.getProblemName(), pe.getLevelId());
        }
        return res;
    }

    @Override
    public ModelGetContestPageResponse getAllPublicContests() {
        List<ModelGetContestResponse> publicContests = contestRepo.findByContestPublicTrue()
                                                                  .stream()
                                                                  .map(contest -> ModelGetContestResponse
                                                                      .builder()
                                                                      .contestId(contest.getContestId())
                                                                      .contestName(contest.getContestName())
//                                                                                .contestTime(contest.getContestSolvingTime())
//                                                                                .countDown(contest.getCountDown())
//                                                                                .startAt(contest.getStartedAt())
                                                                      .statusId(contest.getStatusId())
                                                                      .userId(contest.getUserId())
                                                                      .createdAt(contest.getCreatedAt())
                                                                      .build())
                                                                  .collect(Collectors.toList());

        return ModelGetContestPageResponse.builder()
                                          .contests(publicContests)
                                          .build();
    }

    @Override
    public ModelGetContestPageResponse getAllPublicContestsForParticipant() {
        List<ModelGetContestResponse> publicContests = contestRepo.findPublicContestsForParticipant()
                                                                  .stream()
                                                                  .map(contest -> ModelGetContestResponse
                                                                      .builder()
                                                                      .contestId(contest.getContestId())
                                                                      .contestName(contest.getContestName())
//                                                                                .contestTime(contest.getContestSolvingTime())
//                                                                                .countDown(contest.getCountDown())
//                                                                                .startAt(contest.getStartedAt())
                                                                      .statusId(contest.getStatusId())
                                                                      .userId(contest.getUserId())
//                                                                                .createdAt(contest.getCreatedAt())
                                                                      .build())
                                                                  .collect(Collectors.toList());

        return ModelGetContestPageResponse.builder()
                                          .contests(publicContests)
                                          .build();
    }
}
