package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.exception.MiniLeetCodeException;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.ContestProblemModelResponse;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.SubmissionModelResponse;
import com.hust.baseweb.model.ProblemFilter;
import com.hust.baseweb.model.SubmissionFilter;
import com.hust.baseweb.model.TestCaseFilter;
import com.hust.baseweb.model.dto.ProblemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProblemTestCaseService {

//    ModelGetTestCaseResultResponse getTestCaseResult(
//        String problemId,
//        String userName,
//        ModelGetTestCaseResult modelGetTestCaseResult
//    ) throws Exception;

    ModelCheckCompileResponse checkCompile(ModelCheckCompile modelCheckCompile, String userName) throws Exception;

//    TestCaseEntity saveTestCase(String problemId, ModelSaveTestcase modelSaveTestcase);

    ContestEntity createContest(ModelCreateContest modelCreateContest, String userName) throws Exception;

    ContestEntity updateContest(
        ModelUpdateContest modelUpdateContest,
        String userName,
        String contestId
    ) throws Exception;

    ContestProblem saveProblemInfoInContest(
        ModelProblemInfoInContest modelProblemInfoInContest,
        String userName
    ) throws Exception;

    void removeProblemFromContest(String contestId, String problemId, String userName);

    ModelGetContestDetailResponse getContestDetailByContestIdAndTeacher(String contestId, String userName);


    List<SubmissionDetailByTestcaseOM> getSubmissionDetailByTestcase(UUID submissionId, UUID testcaseId);

    ContestSubmissionEntity teacherDisableSubmission(String userId, UUID submissionId);

    ContestSubmissionEntity teacherEnableSubmission(String userId, UUID submissionId);


    List<SubmissionDetailByTestcaseOM> getParticipantSubmissionDetailByTestCase(
        String userId, UUID submissionId
    );

    void sendSubmissionToQueue(ContestSubmissionEntity submission);

//    ModelContestSubmissionResponse submitSolutionOutput(
//        String solutionOutput,
//        String contestId,
//        String problemId,
//        UUID testCaseId,
//        String userName
//    ) throws Exception;
//
//    ModelContestSubmissionResponse submitSolutionOutputOfATestCase(
//        String userId,
//        String solutionOutput,
//        ModelSubmitSolutionOutputOfATestCase m
//    );

    ModelStudentRegisterContestResponse studentRegisterContest(
        String contestId,
        String userId
    ) throws MiniLeetCodeException;

    int teacherManageStudentRegisterContest(
        String teacherId,
        ModelTeacherManageStudentRegisterContest modelTeacherManageStudentRegisterContest
    ) throws MiniLeetCodeException;

    boolean approveRegisteredUser2Contest(
        String teacherId,
        ModelApproveRegisterUser2ContestInput input
    ) throws MiniLeetCodeException;

    ModelGetContestPageResponse getAllContestsPagingByAdmin(String userName, Pageable pageable);

    List<ModelGetContestResponse> getManagedContestOfTeacher(String userName);

    List<ModelGetContestResponse> getAllContests(String userName);

    ListModelUserRegisteredContestInfo getListUserRegisterContestSuccessfulPaging(Pageable pageable, String contestId);

    List<ContestMembers> getListMemberOfContest(String contestId);

    List<ModelMemberOfContestResponse> getListMemberOfContestGroup(String contestId, String userId);

    ListModelUserRegisteredContestInfo getListUserRegisterContestPendingPaging(Pageable pageable, String contestId);

    List<ModelMemberOfContestResponse> getPendingRegisteredUsersOfContest(String contestId);

    ModelGetContestPageResponse getRegisteredContestsByUser(String userName);

    ModelGetContestPageResponse getNotRegisteredContestByUser(Pageable pageable, String userName);

    List<ContestSubmissionsByUser> getRankingByContestIdNew(
        String contestId,
        Constants.GetPointForRankingType getPointForRankingType
    );

    List<ContestSubmissionsByUser> getRankingGroupByContestIdNew(
        String userId,
        String contestId,
        Constants.GetPointForRankingType getPointForRankingType
    );

//    Page<ProblemEntity> getPublicProblemPaging(Pageable pageable);

    Page<ModelGetTestCaseDetail> getTestCaseByProblem(String problemId, TestCaseFilter filter);

    TestCaseDetailProjection getTestCaseDetail(UUID testCaseId);

//    void editTestCase(UUID testCaseId, ModelSaveTestcase modelSaveTestcase) throws MiniLeetCodeException;

    ModelAddUserToContestResponse addUserToContest(ModelAddUserToContest modelAddUserToContest);

    ModelAddUserToContestResponse updateUserFullnameOfContest(ModelAddUserToContest modelAddUserToContest);

    void addUsers2ToContest(String contestId, AddUsers2Contest addUsers2Contest);

    ModelAddUserToContestGroupResponse addUserToContestGroup(ModelAddUserToContestGroup modelAddUserToContestGroup);

    void deleteUserContest(ModelAddUserToContest modelAddUserToContest) throws MiniLeetCodeException;

    Page<ContestSubmission> findContestSubmissionByContestIdPaging(
        String contestId,
        SubmissionFilter filter
    );




    Page<ContestSubmission> findContestGroupSubmissionByContestIdPaging(
        Pageable pageable,
        String contestId,
        String userId,
        String searchTerm
    );

    Page<ContestSubmission> findContestSubmissionByUserLoginIdPaging(Pageable pageable, String userLoginId);

    Page<ContestSubmission> findContestSubmissionByUserLoginIdAndContestIdPaging(
        Pageable pageable,
        String userLoginId,
        String contestId
    );

    Page<ContestSubmission> findContestSubmissionByUserLoginIdAndContestIdAndProblemIdPaging(
        Pageable pageable,
        String userLoginId,
        String contestId,
        String problemId
    );

    List<ContestSubmission> getNewestSubmissionResults(String userLoginId);

    ContestSubmissionEntity getContestSubmissionDetailForTeacher(UUID submissionId);

    ModelGetContestInfosOfSubmissionOutput getContestInfosOfASubmission(UUID submissionId);

    void deleteTestcase(UUID testcaseId, String userId) throws MiniLeetCodeException;

    ModelCodeSimilarityOutput checkSimilarity(String contestId, ModelCheckSimilarityInput I);

    ModelCodeSimilarityOutput computeSimilarity(String userLoginId, String contestId, ModelCheckSimilarityInput I);

    int checkForbiddenInstructions(String contestId);

    ModelEvaluateBatchSubmissionResponse reJudgeAllSubmissionsOfContest(String contestId);

    void evaluateSubmission(UUID submisionId);

    void evaluateSubmissions(String contestId, String problemId);

    void evaluateSubmission(ContestSubmissionEntity sub, ContestEntity contest);

    void evaluateSubmissionUsingQueue(ContestSubmissionEntity submission);

//    List<CodePlagiarism> findAllByContestId(String contestId);

    List<CodePlagiarism> findAllBy(ModelGetCodeSimilarityParams input);

    List<ModelSimilarityClusterOutput> computeSimilarityClusters(ModelGetCodeSimilarityParams input);

    List<ModelReponseCodeSimilaritySummaryParticipant> getListModelReponseCodeSimilaritySummaryParticipant(String contestId);

    ContestSubmissionEntity updateContestSubmissionSourceCode(ModelUpdateContestSubmission input);

    List<ModelGetContestResponse> getContestsUsingAProblem(String problemId);

    Object addTestcase(
        String testCase,
        ModelProgrammingContestUploadTestCase modelUploadTestCase
    ) throws Exception;

    Object reCreateTestcaseCorrectAnswer(String problemId, UUID testCaseId) throws Exception;

    Object editTestcase(
        UUID testCaseId,
        String testcaseContent,
        ModelProgrammingContestUploadTestCase modelUploadTestCase
    ) throws Exception;

    byte[] getUserJudgedProblemSubmissions(String contestId);

    ModelGetRolesOfUserInContestResponse getRolesOfUserInContest(String userId, String contestId);

    boolean removeMemberFromContest(UUID id);

    boolean removeMemberFromContestGroup(String contestId, String userId, String participantId);


    boolean updatePermissionMemberToContest(String userId, ModelUpdatePermissionMemberToContestInput input);

    List<TagEntity> getAllTags();

    TagEntity addNewTag(ModelTag tag);

//    TagEntity updateTag(Integer tagId, ModelTag tag);

//    void deleteTag(Integer tagId);

    void switchAllContestJudgeMode(String judgeMode);

    List<ContestProblemModelResponse> extApiGetAllProblems(String userID);

    ModelGetContestPageResponse getAllPublicContests();
}
