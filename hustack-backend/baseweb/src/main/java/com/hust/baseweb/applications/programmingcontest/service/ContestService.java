package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContestService {

    TestCaseRepo testCaseRepo;
    ContestRepo contestRepo;

    UserRegistrationContestRepo userRegistrationContestRepo;

    ContestProblemRepo contestProblemRepo;

    ContestSubmissionRepo contestSubmissionRepo;

    static final String HASH_CONTEST = "CONTEST";

    @Cacheable(value = HASH_CONTEST, key = "#contestId")
    public ContestEntity findContestWithCache(String contestId) {
        return findContest(contestId);
    }

    @CachePut(value = HASH_CONTEST, key = "#contest.contestId")
    public ContestEntity updateContestWithCache(ContestEntity contest) {
        return updateContest(contest);
    }

    @CachePut(value = HASH_CONTEST, key = "#result.contestId")
    public ContestEntity saveContestWithCache(ContestEntity contest) {
        return saveContest(contest);
    }

    public ContestEntity findContest(String contestId) {
        return contestRepo.findContestByContestId(contestId);
    }

    public ContestEntity updateContest(ContestEntity contest) {
        return contestRepo.save(contest);
    }

    public ContestEntity saveContest(ContestEntity contest) {
        return contestRepo.save(contest);
    }

    public ModelGetContestDetailResponse getModelGetContestDetailResponse(ContestEntity contestEntity) {
        String contestId = contestEntity.getContestId();
        List<ModelGetProblemDetailResponse> problems = new ArrayList<>();


        // Ensure problem list is not null
        if (contestEntity.getProblems() != null) {
            contestEntity.getProblems().forEach(contestProblem -> {
                ContestProblem cp = contestProblemRepo.findByContestIdAndProblemId(
                    contestEntity.getContestId(),
                    contestProblem.getProblemId());

                // Initialize default values
                String submissionMode = "";
                String problemRename = "";
                String problemRecode = "";
                String forbiddenInstructions = "";
                Double coefficientPoint = 1.0;

                // If contest problem exists in the repository, update values
                if (cp != null) {
                    submissionMode = cp.getSubmissionMode();
                    problemRename = cp.getProblemRename();
                    problemRecode = cp.getProblemRecode();
                    forbiddenInstructions = cp.getForbiddenInstructions();
                    coefficientPoint = cp.getCoefficientPoint();
                }
                TestCaseStatsDTO stats = testCaseRepo.getStatsByProblemId(contestProblem.getProblemId());
                int testCasesCount = stats != null ? stats.getTestCasesCount() : 0;
                int totalPointTestCase = stats != null ? stats.getTotalPointTestCase() : 0;


                ModelGetProblemDetailResponse p = ModelGetProblemDetailResponse.builder()
                                                                               .levelId(contestProblem.getLevelId())
                                                                               .problemId(contestProblem.getProblemId())
                                                                               .problemName(contestProblem.getProblemName())
                                                                               .problemRename(problemRename)
                                                                               .problemRecode(problemRecode)
                                                                               .forbiddenInstructions(forbiddenInstructions)
                                                                               .coefficientPoint(coefficientPoint)
                                                                               .levelOrder(contestProblem.getLevelOrder())
                                                                               .problemDescription(contestProblem.getProblemDescription())
                                                                               .createdByUserId(contestProblem.getCreatedBy())
                                                                               .testCasesCount(testCasesCount)
                                                                               .totalPointTestCase(totalPointTestCase)
                                                                               .submissionMode(submissionMode)
                                                                               .build();

                problems.add(p);
            });
        }

        return ModelGetContestDetailResponse
            .builder()
            .contestId(contestId)
            .contestName(contestEntity.getContestName())
            .contestTime(contestEntity.getContestSolvingTime())
            .startAt(contestEntity.getStartedAt())
            .list(problems)
            .unauthorized(false)
            .statusId(contestEntity.getStatusId())
            .listStatusIds(ContestEntity.getStatusIds())
            .listSubmissionActionTypes(ContestEntity.getSubmissionActionTypes())
            .listParticipantViewModes(ContestEntity.getParticipantViewResultModes())
            .listProblemDescriptionViewTypes(ContestEntity.getProblemDescriptionViewTypes())
            .listJudgeModes(ContestEntity.getJudgeModes())
            .listEvaluateBothPublicPrivateTestcases(ContestEntity.getListEvaluateBothPublicPrivateTestcases())
            .submissionActionType(contestEntity.getSubmissionActionType())
            .maxNumberSubmission(contestEntity.getMaxNumberSubmissions())
            .participantViewResultMode(contestEntity.getParticipantViewResultMode())
            .problemDescriptionViewType(contestEntity.getProblemDescriptionViewType())
            .evaluateBothPublicPrivateTestcase(contestEntity.getEvaluateBothPublicPrivateTestcase())
            .maxSourceCodeLength(contestEntity.getMaxSourceCodeLength())
            .minTimeBetweenTwoSubmissions(contestEntity.getMinTimeBetweenTwoSubmissions())
            .judgeMode(contestEntity.getJudgeMode())
            .participantViewSubmissionMode(contestEntity.getParticipantViewSubmissionMode())
            .listParticipantViewSubmissionModes(ContestEntity.getListParticipantViewSubmissionModes())
            .languagesAllowed(contestEntity.getLanguagesAllowed())
            .listLanguagesAllowed(contestEntity.getListLanguagesAllowed())
            .contestType(contestEntity.getContestType())
            .listContestTypes(ContestEntity.getListContestTypes())
            .contestShowTag(contestEntity.getContestShowTag())
            .listContestShowTags(contestEntity.getListContestShowTag())
            .contestShowComment(contestEntity.getContestShowComment())
            .listContestShowComments(contestEntity.getListContestShowComment())
            .contestPublic(contestEntity.getContestPublic())
            .listContestPublic(contestEntity.getListContestPublic())
            .allowParticipantPinSubmission(contestEntity.getAllowParticipantPinSubmission())
            .canEditCoefficientPoint(contestEntity.getCanEditCoefficientPoint())

            .build();
    }

    @Transactional
    public void updateContestSubmissionStatus(UUID submissionId, String status) {
        contestSubmissionRepo.updateContestSubmissionStatus(submissionId, status);
    }

    public String getProblemNameInContest(String contestId, String problemId) {
        ContestProblem cp = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
        return cp != null ? cp.getProblemRename() : null;
    }

    public int mapNewProblemToSubmissionsInContest(ModelInputMapNewProblemToSubmissionsInContest m) {
        List<ContestSubmissionEntity> L = contestSubmissionRepo.findAllByContestIdAndProblemId(
            m.getContestId(),
            m.getProblemId());

        for (ContestSubmissionEntity sub : L) {
            sub.setProblemId(
                m.getNewProblemId()
            );
            contestSubmissionRepo.save(sub);
        }

        return L.size();
    }

    @Transactional
    public ModelGetContestResponse cloneContest(String userId, ModelInputCloneContest m) {
        ContestEntity fromContest = contestRepo.findContestByContestId(m.getFromContestId());
        if (fromContest == null) {
            return null;
        }

        ContestEntity contest = ContestEntity.builder()
                                             .contestId(m.getToContestId())
                                             .contestName(m.getToContestName())
                                             .contestPublic(fromContest.getContestPublic())
                                             .contestType(fromContest.getContestType())
                                             .contestSolvingTime(fromContest.getContestSolvingTime())
                                             .sendConfirmEmailUponSubmission(fromContest.getSendConfirmEmailUponSubmission())
                                             .languagesAllowed(fromContest.getLanguagesAllowed())
                                             .endTime(fromContest.getEndTime())
                                             .createdAt(new Date())
                                             .judgeMode(fromContest.getJudgeMode())
                                             .contestShowTag(fromContest.getContestShowTag())
                                             .countDown(fromContest.getCountDown())
                                             .contestShowComment(fromContest.getContestShowComment())
                                             .participantViewSubmissionMode(fromContest.getParticipantViewSubmissionMode())
                                             .maxNumberSubmissions(fromContest.getMaxNumberSubmissions())
                                             .problems(fromContest.getProblems())
                                             .userId(userId)
                                             .evaluateBothPublicPrivateTestcase(fromContest.getEvaluateBothPublicPrivateTestcase())
                                             .maxSourceCodeLength(fromContest.getMaxSourceCodeLength())
                                             .minTimeBetweenTwoSubmissions(fromContest.getMinTimeBetweenTwoSubmissions())
                                             .problemDescriptionViewType(fromContest.getProblemDescriptionViewType())
                                             .statusId(ContestEntity.CONTEST_STATUS_CREATED)
                                             .submissionActionType(fromContest.getSubmissionActionType())
                                             .build();

        contest = contestRepo.save(contest);

        // clone problems in contest
        List<ContestProblem> problems = contestProblemRepo.findAllByContestId(m.getFromContestId());
        for (ContestProblem p : problems) {
            ContestProblem cp = ContestProblem.builder()
                                              .contestId(m.getToContestId())
                                              .forbiddenInstructions(p.getForbiddenInstructions())
                                              .problemId(p.getProblemId())
                                              .problemRecode(p.getProblemRecode())
                                              .problemRename(p.getProblemRename())
                                              .submissionMode(p.getSubmissionMode())
                                              .build();
            contestProblemRepo.save(cp);
        }

        // clone submissions
        List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestId(m.getFromContestId());
        for (ContestSubmissionEntity sub : submissions) {
            ContestSubmissionEntity cloneSub = ContestSubmissionEntity.builder()
                                                                      .contestId(m.getToContestId())
                                                                      .point(sub.getPoint())
                                                                      .runtime(sub.getRuntime())
                                                                      .memoryUsage(sub.getMemoryUsage())
                                                                      .message(sub.getMessage())
                                                                      .problemId(sub.getProblemId())
                                                                      .sourceCode(sub.getSourceCode())
                                                                      .memoryUsage(sub.getMemoryUsage())
                                                                      .sourceCodeLanguage(sub.getSourceCodeLanguage())
                                                                      .submittedByUserId(sub.getSubmittedByUserId())
                                                                      .testCasePass(sub.getTestCasePass())
                                                                      .managementStatus(sub.getManagementStatus())
                                                                      .status(sub.getStatus())
                                                                      .userId(sub.getUserId())
                                                                      .submittedByUserId(sub.getSubmittedByUserId())
                                                                      .violateForbiddenInstruction(sub.getViolateForbiddenInstruction())
                                                                      .violateForbiddenInstructionMessage(sub.getViolateForbiddenInstructionMessage())
                                                                      .createdAt(sub.getCreatedAt())
                                                                      .build();
            contestSubmissionRepo.save(cloneSub);
        }

        // clone contest user roles, table user_registration_contest_new
        List<UserRegistrationContestEntity> userRegis = userRegistrationContestRepo.findAllByContestId(m.getFromContestId());
        for (UserRegistrationContestEntity u : userRegis) {
            UserRegistrationContestEntity ure = UserRegistrationContestEntity.builder()
                                                                             .contestId(m.getToContestId())
                                                                             .fullname(u.getFullname())
                                                                             .roleId(u.getRoleId())
                                                                             .status(u.getStatus())
                                                                             .createdStamp(u.getCreatedStamp())
                                                                             .lastUpdated(u.getLastUpdated())
                                                                             .permissionId(u.getPermissionId())
                                                                             .userId(u.getUserId())
                                                                             .updatedByUserLogin_id(u.getUpdatedByUserLogin_id())
                                                                             .build();
            userRegistrationContestRepo.save(ure);
        }


        return ModelGetContestResponse.builder()
                                      .contestId(contest.getContestId())
                                      .build();
    }
}
