package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.SubmissionModelResponse;
import com.hust.baseweb.model.ProblemFilter;
import com.hust.baseweb.model.TestCaseFilter;
import com.hust.baseweb.model.dto.ProblemDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ProblemService {

    ProblemEntity createProblem(String userID, CreateProblemDTO dto, MultipartFile[] files);

    void editProblem(
        String problemId,
        String userId,
        EditProblemDTO dto,
        MultipartFile[] files
    );

    List<ModelProblemGeneralInfo> getAllProblemsGeneralInfo();

    Page<ProblemDTO> getProblems(String ownerId, ProblemFilter filter, Boolean isPublic);

    void cloneProblem(String userId, CloneProblemDTO cloneRequest);

    Page<ProblemDTO> getPublicProblems(String userId, ProblemFilter filter);

    List<SubmissionModelResponse> extApiGetSubmissions(String participantId);

    Page<ProblemDTO> getSharedProblems(String userId, ProblemFilter filter);

    List<ModelImportProblemFromContestResponse> importProblemsFromAContest(
        String userId,
        ImportProblemsFromAContestDTO I
    );

    ModelCreateContestProblemResponse getProblemDetailForManager(
        String problemId,
        String userId
    ) throws Exception;

    void exportProblem(String id, String userId, OutputStream outputStream);

    ProblemDetailForParticipantDTO getProblemDetailForParticipant(String userId, String contestId, String problemId);

    List<ModelStudentOverviewProblem> getListProblemsInContestForParticipant(String userId, String contestId);

    AttachmentMetadata downloadProblemAttachment(String userId, String contestId, String problemId, String fileId);

    AttachmentMetadata downloadProblemAttachmentForTeacher(String userId, String problemId, String fileId);

    List<UserProblemRoleDTO> getProblemPermissions(String problemId, String userId);
    
    Map<String, Object> grantProblemPermission(String userName, GrantProblemPermissionDTO input);
    
    boolean revokeProblemPermission(String userName, RevokeProblemPermissionDTO input);

    Page<ModelGetTestCaseDetail> getTestCaseByProblem(String problemId, String userId, TestCaseFilter filter);
}
