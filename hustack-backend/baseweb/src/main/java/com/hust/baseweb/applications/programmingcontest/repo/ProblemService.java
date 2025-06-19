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
import java.util.UUID;

public interface ProblemService {
    ProblemEntity createContestProblem(String userID, ModelCreateContestProblem dto, MultipartFile[] files);

    ProblemEntity updateContestProblem(
        String problemId,
        String userId,
        ModelUpdateContestProblem dto,
        MultipartFile[] files
    ) throws Exception;

    List<ModelStudentOverviewProblem> getStudentContestProblems(String userId, String contestId);

    List<ModelProblemGeneralInfo> getAllProblemsGeneralInfo();

    void exportProblemJson(String problemId, OutputStream outputStream, String userId);

    void importProblem(ModelImportProblem model, MultipartFile zipFile, String userId);


    boolean removeUserProblemRole(String userName, ModelUserProblemRole input) throws Exception;

    Map<String, Object> addUserProblemRole(String userName, ModelUserProblemRoleInput input) throws Exception;

    Page<ProblemDTO> getProblems(String ownerId, ProblemFilter filter, Boolean isPublic);

    ProblemEntity cloneProblem(String userId, ModelCloneProblem cloneRequest) throws MiniLeetCodeException;

    Page<ProblemDTO> getPublicProblems(String userId, ProblemFilter filter);

    List<SubmissionModelResponse> extApiGetSubmissions(String participantId);

    List<ProblemEntity> getAllProblems(String userId);

    Page<ProblemDTO> getSharedProblems(String userId, ProblemFilter filter);

    List<ModelImportProblemFromContestResponse> importProblemsFromAContest(ModelImportProblemsFromAContestInput I);

    ModelCreateContestProblemResponse getContestProblemDetailByIdAndTeacher(
        String problemId,
        String teacherId
    ) throws Exception;

    void exportProblem(String id, OutputStream outputStream);

    List<ModelResponseUserProblemRole> getUserProblemRoles(String problemId);

    ModelCreateContestProblemResponse getContestProblem(String problemId) throws Exception;
}
