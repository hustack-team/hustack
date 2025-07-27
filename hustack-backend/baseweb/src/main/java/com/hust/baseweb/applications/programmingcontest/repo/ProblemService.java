package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.exception.MiniLeetCodeException;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.SubmissionModelResponse;
import com.hust.baseweb.model.ProblemFilter;
import com.hust.baseweb.model.dto.ProblemDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ProblemService {

    ProblemEntity createProblem(String userID, ModelCreateContestProblem dto, MultipartFile[] files);

    ProblemEntity editProblem(
        String problemId,
        String userId,
        ModelUpdateContestProblem dto,
        MultipartFile[] files
    );

    List<ModelProblemGeneralInfo> getAllProblemsGeneralInfo();

    boolean removeAUserProblemRole(String userName, ModelUserProblemRole input) throws Exception;

    Map<String, Object> addUserProblemRole(String userName, ModelUserProblemRoleInput input) throws Exception;

    Page<ProblemDTO> getProblems(String ownerId, ProblemFilter filter, Boolean isPublic);

    ProblemEntity cloneProblem(String userId, CloneProblemDTO cloneRequest) throws MiniLeetCodeException;

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

    void exportProblem(String id, OutputStream outputStream);

    List<ModelResponseUserProblemRole> getUserProblemRoles(String problemId, String userId);

    ProblemDetailForParticipantDTO getProblemDetailForParticipant(String userId, String contestId, String problemId);

    List<ModelStudentOverviewProblem> getListProblemsInContestForParticipant(String userId, String contestId);

    AttachmentMetadata downloadProblemAttachment(String userId, String contestId, String problemId, String fileId);

    AttachmentMetadata downloadProblemAttachmentForTeacher(String userId, String problemId, String fileId);
}
