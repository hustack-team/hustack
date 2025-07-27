package com.hust.baseweb.applications.programmingcontest.controller;

import com.google.gson.Gson;
import com.hust.baseweb.applications.programmingcontest.callexternalapi.model.LmsLogModelCreate;
import com.hust.baseweb.applications.programmingcontest.callexternalapi.service.ApiService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import com.hust.baseweb.applications.programmingcontest.entity.ContestSubmissionEntity;
import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.entity.UserRegistrationContestEntity;
import com.hust.baseweb.applications.programmingcontest.exception.MiniLeetCodeException;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.ContestRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ContestSubmissionRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemService;
import com.hust.baseweb.applications.programmingcontest.service.ContestService;
import com.hust.baseweb.applications.programmingcontest.service.ProblemTestCaseService;
import com.hust.baseweb.model.SubmissionFilter;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContestController {

    ProblemService problemService;

    ProblemTestCaseService problemTestCaseService;

    ContestRepo contestRepo;

    ContestSubmissionRepo contestSubmissionRepo;

    ProblemRepo problemRepo;

    ContestService contestService;

    ApiService apiService;

    @Secured("ROLE_TEACHER")
    @PostMapping("/map-new-problem-to-submissions-in-contest")
    public ResponseEntity<?> mapNewProblemToSubmissionsInContest(
        Principal
            principal, @RequestBody ModelInputMapNewProblemToSubmissionsInContest m
    ) {
        int cnt = contestService.mapNewProblemToSubmissionsInContest(m);
        return ResponseEntity.ok().body(cnt);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher-get-problem-detail-in-contest/{contestId}/{problemId}")
    public ResponseEntity<?> teacherGetProblemDetailInContest(
        Principal principal,
        @PathVariable String contestId,
        @PathVariable String problemId
    ) {
        log.info("teacherGetProblemDetailInContest, contestId  " + contestId + " problemId = " + problemId);
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        ModelRepsonseTeacherGetProblemDetailInContest res = new ModelRepsonseTeacherGetProblemDetailInContest();

        if (problem != null) {
            res.setProblemId(problem.getProblemId());
            res.setProblemName(problem.getProblemName());
            res.setProblemDescription(problem.getProblemDescription());
            res.setSolutionCode(problem.getCorrectSolutionSourceCode());
        }

        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher-get-submissions-of-problem-in-contest/{contestId}/{problemId}")
    public ResponseEntity<?> teacherGetSubmissionsOfProblemInContest(
        Principal principal,
        @PathVariable String contestId,
        @PathVariable String problemId
    ) {
        ContestEntity contest = contestRepo.findContestByContestId(contestId);

        List<ContestSubmissionEntity> submissions = contestSubmissionRepo.findAllByContestIdAndProblemId(
            contestId,
            problemId);
        log.info("teacherGetSubmissionsOfProblemInContest, list.sz = " + submissions.size());

        return ResponseEntity.ok().body(submissions);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/clone-contest")
    public ResponseEntity<?> cloneContest(Principal principal, @RequestBody ModelInputCloneContest m) {
        log.info("clone Contest, fromContestId = " +
                 m.getFromContestId() +
                 " toContestId = " +
                 m.getToContestId() +
                 " toContestName = " +
                 m.getToContestName());
        ModelGetContestResponse res = contestService.cloneContest(principal.getName(), m);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests")
    public ResponseEntity<?> createContest(
        @RequestBody @Valid ModelCreateContest modelCreateContest,
        Principal principal
    )
        throws Exception {
        log.info("createContest {}", modelCreateContest);
        ContestEntity contest = problemTestCaseService.createContest(modelCreateContest, principal.getName());
        //ContestEntity contest = problemTestCaseService.createContest(modelCreateContest, principal.getName());
        return ResponseEntity.ok().body(contest);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/import-problems")
    public ResponseEntity<?> importProblemsFromAContest(
        Principal principal,
        @RequestBody ImportProblemsFromAContestDTO input
    ) {

        try {
            List<ModelImportProblemFromContestResponse> res = problemService.importProblemsFromAContest(
                principal.getName(),
                input);
            return ResponseEntity.ok().body(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Async
    public void logUpdateContest(String userId, String contestId, ModelUpdateContest modelUpdateContest) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logUpdateContest, userId = " + logM.getUserId());
        logM.setParam1(contestId);
        logM.setParam2(modelUpdateContest.getStatusId());
        logM.setParam3(modelUpdateContest.getContestName());

        logM.setActionType("MANAGER_UPDATE_CONTEST");
        logM.setDescription("an user update a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @Secured("ROLE_TEACHER")
    @PutMapping("/contests/{contestId}")
    public ResponseEntity<?> editContest(
        @RequestBody ModelUpdateContest modelUpdateContest, Principal principal,
        @PathVariable("contestId") String contestId
    ) throws Exception {
        log.info("edit contest modelUpdateContest {}", modelUpdateContest);

        try {
            logUpdateContest(principal.getName(), contestId, modelUpdateContest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        problemTestCaseService.updateContest(modelUpdateContest, principal.getName(), contestId);
        return ResponseEntity.ok().body(null);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contest-problem")
    public ResponseEntity<?> saveProblemToContest(
        @RequestBody ModelProblemInfoInContest modelProblemInfoInContest,
        Principal principal
    ) throws Exception {

        problemTestCaseService.saveProblemInfoInContest(modelProblemInfoInContest, principal.getName());

        return ResponseEntity.ok().body("ok");
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/contest-problem")
    public ResponseEntity<?> removeProblemFromContest(
        @RequestParam String contestId,
        @RequestParam String problemId,
        Principal principal
    ) {

        problemTestCaseService.removeProblemFromContest(contestId, problemId, principal.getName());

        return ResponseEntity.ok().body("ok");
    }

    @GetMapping("/contests/roles")
    public ResponseEntity<?> getListRolesContest() {
        List<String> L = UserRegistrationContestEntity.getListRoles();
        return ResponseEntity.ok().body(L);
    }

    @Async
    public void logGetContestDetail(String userId, String contestId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logGetContestDetail, userId = " + logM.getUserId());
        logM.setParam1(contestId);

        logM.setActionType("MANAGER_GET_CONTEST_DETAIL");
        logM.setDescription("an user get detail of a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @GetMapping("/contests/{contestId}")
    public ResponseEntity<?> getContestDetail(@PathVariable("contestId") String contestId, Principal principal) {
        log.info("getContestDetail constestid {}", contestId);

        //logGetContestDetail(principal.getName(),contestId);

        ModelGetContestDetailResponse response = problemTestCaseService.getContestDetailByContestIdAndTeacher(
            contestId,
            principal.getName());
        return ResponseEntity.ok().body(response);
    }

    @Async
    public void logStudentGetProblemOfContestForSolving(String userId, String contestId, String problemId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logStudentGetProblemOfContestForSolving, userId = " + logM.getUserId());
        logM.setParam1(contestId);
        logM.setParam2(problemId);

        logM.setActionType("STUDENT_GET_A_PROBLEM_OF_CONTEST_FOR_SOLVING");
        logM.setDescription("an user get a problem of a contest for solving");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }


    @GetMapping("/contests/{contestId}/problems/{problemId}")
    public ResponseEntity<?> getProblemDetailInContestForParticipant(
        Principal principal,
        @PathVariable("problemId") String problemId,
        @PathVariable("contestId") String contestId
    ) {
        logStudentGetProblemOfContestForSolving(principal.getName(), contestId, problemId);
        ProblemDetailForParticipantDTO result = problemService.getProblemDetailForParticipant(
            principal.getName(), contestId, problemId
        );
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Problem not found");
        }
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/contests/{contestId}/problems")
    public ResponseEntity<?> getListContestProblemViewedByStudent(@PathVariable("contestId") String contestId) {
        ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
        List<ProblemEntity> listProblem = contestEntity.getProblems();
        return ResponseEntity.ok().body(listProblem);
    }

    @Async
    public void logStudentGetDetailContest(String userId, String contestId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logStudentGetDetailContest, userId = " + logM.getUserId());
        logM.setParam1(contestId);


        logM.setActionType("STUDENT_GET_DETAIL_CONTEST");
        logM.setDescription("an student get detail of a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @GetMapping("/contests/{contestId}/problems/v2")
    public ResponseEntity<?> getListContestProblemViewedByStudentV2(
        @PathVariable("contestId") String contestId,
        Principal principal
    ) {
        String userId = principal.getName();
        logStudentGetDetailContest(userId, contestId);

        List<ModelStudentOverviewProblem> responses =
            problemService.getListProblemsInContestForParticipant(userId, contestId);

        return ResponseEntity.ok().body(responses);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/admin/contests")
    public ResponseEntity<?> getAllContestPagingByAdmin(
        Principal principal, Pageable pageable,
        @Param("sortBy") String sortBy
    ) {
        if (sortBy != null) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sortBy));
        } else {
            pageable = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("createdAt").ascending());
        }
        ModelGetContestPageResponse resp = problemTestCaseService.getAllContestsPagingByAdmin(
            principal.getName(),
            pageable);
        return ResponseEntity.ok().body(resp);
    }

    @Async
    public void logTeacherGetMyContest(String userId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logTeacherGetMyContest, userId = " + logM.getUserId());

        logM.setActionType("TEACHER_GET_MY_CONTESTS");
        logM.setDescription("an manager (teacher) get his contests");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests")
    public ResponseEntity<?> getManagedContestOfTeacher(Principal principal) {
        List<ModelGetContestResponse> resp = problemTestCaseService
            .getManagedContestOfTeacher(principal.getName());

        logTeacherGetMyContest(principal.getName());

        return ResponseEntity.ok().body(resp);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/all-contests")
    public ResponseEntity<?> getAllContest(Principal principal) {
        List<ModelGetContestResponse> resp = problemTestCaseService
            .getAllContests(principal.getName())
            .stream()
            .filter(contestResponse -> !contestResponse.getStatusId().equals("DISABLED"))
            .collect(Collectors.toList());
        return ResponseEntity.ok().body(resp);
    }

    @PostMapping("contests/{contestId}/register-student")
    public ResponseEntity<?> studentRegisterContest(@PathVariable("contestId") String contestId, Principal principal)
        throws MiniLeetCodeException {
        log.info("studentRegisterContest {}", contestId);
        ModelStudentRegisterContestResponse resp = problemTestCaseService.studentRegisterContest(
            contestId,
            principal.getName());
        return ResponseEntity.ok().body(resp);
    }

    //@Secured("ROLE_TEACHER")
    @GetMapping("/contests/{contestId}/registered-users")
    public ResponseEntity<?> getUserRegisterSuccessfulContest(
        @PathVariable("contestId") String contestId,
        Pageable pageable
    ) {
        log.info("get User Register Successful Contest ");
        ListModelUserRegisteredContestInfo resp = problemTestCaseService
            .getListUserRegisterContestSuccessfulPaging(pageable, contestId);
        return ResponseEntity.ok().body(resp);
    }

    //@Secured("ROLE_TEACHER")
    @GetMapping("/contests/{contestId}/members")
    public ResponseEntity<?> getMembersOfContest(@PathVariable String contestId) {
        List<ContestMembers> res = problemTestCaseService.getListMemberOfContest(contestId);
        return ResponseEntity.ok().body(res);
    }

    @GetMapping("/contests/{contestId}/group/members")
    public ResponseEntity<?> getMembersOfContestGroup(Principal principal, @PathVariable String contestId) {
        String userId = principal.getName();
        List<ModelMemberOfContestResponse> res = problemTestCaseService.getListMemberOfContestGroup(contestId, userId);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/contests/members")
    public ResponseEntity<?> removeMemberFromContest(
        @RequestBody ModelRemoveMemberFromContestInput input
    ) {
        boolean res = problemTestCaseService.removeMemberFromContest(input.getId());
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/contests/group/members")
    public ResponseEntity<?> removeMemberFromContestGroup(
        Principal principal,
        @RequestBody ModelRemoveMemberFromContestGroupInput input
    ) {
        String userId = principal.getName();
        boolean res = problemTestCaseService.removeMemberFromContestGroup(
            input.getContestId(),
            userId,
            input.getParticipantId());
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @PutMapping("/contests/permissions")
    public ResponseEntity<?> updatePermissionOfMemberToContest(
        Principal principal,
        @RequestBody ModelUpdatePermissionMemberToContestInput input
    ) {
        boolean res = problemTestCaseService.updatePermissionMemberToContest(principal.getName(), input);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/{contestId}/pending-users")
    public ResponseEntity<?> getUserRegisterPendingContest(
        @PathVariable("contestId") String contestId,
        Pageable pageable, @Param("size") String size, @Param("page") String page
    ) {
        log.info(
            "get User Register Pending Contest pageable {} size {} page {} contest id {}", pageable, size, page,
            contestId);
        ListModelUserRegisteredContestInfo resp = problemTestCaseService
            .getListUserRegisterContestPendingPaging(pageable, contestId);
        return ResponseEntity.ok().body(resp);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/{contestId}/pending-users-v2")
    public ResponseEntity<?> getPendingRegisteredUserOfContest(@PathVariable String contestId) {
        List<ModelMemberOfContestResponse> res = problemTestCaseService.getPendingRegisteredUsersOfContest(contestId);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/registers/approval-management")
    public ResponseEntity<?> teacherManagerStudentRegisterContest(
        Principal principal,
        @RequestBody ModelTeacherManageStudentRegisterContest request
    ) throws MiniLeetCodeException {
        log.info("teacherManagerStudentRegisterContest");
        problemTestCaseService.teacherManageStudentRegisterContest(principal.getName(), request);
        return ResponseEntity.ok().body(null);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/registers/approval")
    public ResponseEntity<?> approveRegisteredUser2Contest(
        Principal principal,
        @RequestBody ModelApproveRegisterUser2ContestInput input
    ) {
        try {
            problemTestCaseService.approveRegisteredUser2Contest(principal.getName(), input);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok().body("");
        }
        return ResponseEntity.ok().body(true);
    }

    @Async
    public void logStudentGetHisContests(String userId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logStudentGetHisContests, userId = " + logM.getUserId());

        logM.setActionType("STUDENT_GET_LIST_HIS_CONTESTS");
        logM.setDescription("an user (participant) get list of his contests");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @Secured("ROLE_STUDENT")
    @GetMapping("/students/contests")
    public ResponseEntity<?> getRegisteredContestsForParticipant(Principal principal) {
        //logStudentGetHisContests(principal.getName());

        ModelGetContestPageResponse res = problemTestCaseService.getRegisteredContestsForParticipant(principal.getName());
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/public")
    public ResponseEntity<ModelGetContestPageResponse> getAllPublicContests() {
        ModelGetContestPageResponse response = problemTestCaseService.getAllPublicContests();
        return ResponseEntity.ok(response);
    }

    @Secured("ROLE_STUDENT")
    @GetMapping("/students/contests/public")
    public ResponseEntity<ModelGetContestPageResponse> getPublicContestsForStudents() {
        ModelGetContestPageResponse response = problemTestCaseService.getAllPublicContestsForParticipant();
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/students/not-registered-contests")
//    public ResponseEntity<?> getContestNotRegisteredByStudentPaging(
//        Pageable pageable, @Param("sortBy") String sortBy,
//        Principal principal
//    ) {
//        log.info("getContestRegisteredByStudentPaging sortBy {} pageable {}", sortBy, pageable);
//        if (sortBy != null) {
//            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sortBy));
//        } else {
//            pageable = PageRequest.of(
//                pageable.getPageNumber(), pageable.getPageSize(),
//                Sort.by("startedAt").descending());
//        }
//        ModelGetContestPageResponse modelGetContestPageResponse = problemTestCaseService
//            .getNotRegisteredContestByUser(pageable, principal.getName());
//        return ResponseEntity.ok().body(modelGetContestPageResponse);
//    }

    @Secured("ROLE_TEACHER")
    @Deprecated
    @PostMapping("/contests/users")
    public ResponseEntity<?> addUserContest(@RequestBody ModelAddUserToContest modelAddUserToContest) {
        problemTestCaseService.addUserToContest(modelAddUserToContest);
        return ResponseEntity.ok().body(null);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/{id}/users")
    public ResponseEntity<?> addUsers2Contest(
        @PathVariable(name = "id") String contestId,
        @RequestBody AddUsers2Contest addUsers2Contest
    ) {
        problemTestCaseService.addUsers2ToContest(contestId, addUsers2Contest);
        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping("/contests/users")
    public ResponseEntity<?> deleteUserFromContest(@RequestBody ModelAddUserToContest modelAddUserToContest)
        throws MiniLeetCodeException {
        problemTestCaseService.deleteUserContest(modelAddUserToContest);
        return ResponseEntity.ok().body(null);
    }

    //    @GetMapping("/public/ranking-programming-contest/{contestId}")
//    public ResponseEntity<?> getRankingContestPublic(@PathVariable("contestId") String contestId, Pageable pageable) {
//        pageable = Pageable.unpaged();
//        List<ContestSubmissionsByUser> page = problemTestCaseService.getRankingByContestIdNew(pageable, contestId,
//                                                                                              Constants.GetPointForRankingType.HIGHEST);
//        // log.info("ranking page {}", page);
//        return ResponseEntity.ok().body(page);
//    }

    @GetMapping("/contests/public-ranking/{contestId}")
    public ResponseEntity<?> getRankingContestPublic(
        @PathVariable("contestId") String contestId,
        @RequestParam Constants.GetPointForRankingType getPointForRankingType
    ) {
        // if (contestSubmissionRepo.countAllByContestId(contestId) > 500) {
        //     return ResponseEntity
        //         .status(400)
        //         .body("This contest size is too big. Contact the contest manager for ranking table");
        // }

        // temporary remove
        /*
        List<ContestSubmissionsByUser> res = problemTestCaseService.getRankingByContestIdNew(
            contestId,
            getPointForRankingType);
        */
        List<ContestSubmissionsByUser> res = new ArrayList<>();

        return ResponseEntity.ok().body(res);
        //return ResponseEntity.ok().body(null);
    }

    @Async
    protected void logGetRankingOfContest(String userId, String contestId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logGetRankingOfContest, userId = " + logM.getUserId());
        logM.setParam1(contestId);

        logM.setActionType("MANAGER_GET_RANKING_OF_A_CONTEST");
        logM.setDescription("an user get ranking of a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/ranking/{contestId}")
    public ResponseEntity<?> getRankingContestNewVersion(
        Principal principal,
        @PathVariable("contestId") String contestId,
        @RequestParam Constants.GetPointForRankingType getPointForRankingType
    ) {
        logGetRankingOfContest(principal.getName(), contestId);

        List<ContestSubmissionsByUser> res = problemTestCaseService.getRankingByContestIdNew(
            contestId,
            getPointForRankingType);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/group/ranking/{contestId}")
    public ResponseEntity<?> getRankingContestGroupNewVersion(
        Principal principal,
        @PathVariable("contestId") String contestId,
        @RequestParam Constants.GetPointForRankingType getPointForRankingType
    ) {
        String userId = principal.getName();
        List<ContestSubmissionsByUser> res = problemTestCaseService.getRankingGroupByContestIdNew(
            userId,
            contestId,
            getPointForRankingType);
        return ResponseEntity.ok().body(res);
    }

    @GetMapping("/contests/{contestId}/users/submissions")
    public ResponseEntity<?> getContestSubmissionPagingOfCurrentUser(
        Principal principal,
        @PathVariable String contestId
    ) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").descending());
        Page<ContestSubmission> page = problemTestCaseService
            .findContestSubmissionByUserLoginIdAndContestIdPaging(pageable, principal.getName(), contestId);
        return ResponseEntity.ok().body(page);
    }

    @GetMapping("/contests/users/submissions")
    public ResponseEntity<?> getContestSubmissionInProblemPagingOfCurrentUser(
        Principal principal,
        @RequestParam("contestId") String contestId, @RequestParam("problemId") String problemId
    ) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").descending());
        Page<ContestSubmission> page = problemTestCaseService
            .findContestSubmissionByUserLoginIdAndContestIdAndProblemIdPaging(
                pageable,
                principal.getName(),
                contestId,
                problemId);
        return ResponseEntity.ok().body(page);
    }

    @Async
    protected void logGetSubmissionsOfContest(String userId, String contestId) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logGetSubmissionsOfContest, userId = " + logM.getUserId());
        logM.setParam1(contestId);

        logM.setActionType("MANAGER_GET_SUBMISSIONS_OF_A_CONTEST");
        logM.setDescription("an user get submissions of a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher/contests/{contestId}/submissions")
    public ResponseEntity<?> getContestSubmissionPaging(
        Principal principal,
        @PathVariable("contestId") String contestId,
        SubmissionFilter filter
    ) {
        logGetSubmissionsOfContest(principal.getName(), contestId);
        Page<ContestSubmission> res = problemTestCaseService.findContestSubmissionByContestIdPaging(contestId, filter);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher/contests/{contestId}/group/submissions")
    public ResponseEntity<?> getContestGroupSubmissionPaging(
        Principal principal,
        @PathVariable("contestId") String contestId,
        @RequestParam String search,
        @RequestParam int page,
        @RequestParam int size
    ) {
        String userId = principal.getName();
        log.info("getContestGroupSubmissionPaging, userId = " + userId);
        Pageable pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ContestSubmission> res = problemTestCaseService.findContestGroupSubmissionByContestIdPaging(
            pageRequest,
            contestId,
            userId,
            search);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/{contestId}/judged-submissions")
    public ResponseEntity<?> getUserJudgedProblemSubmission(@PathVariable String contestId) {
        byte[] pdfBytes = problemTestCaseService.getUserJudgedProblemSubmissions(contestId);
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + contestId + ".pdf")
                             .contentType(MediaType.APPLICATION_PDF)
                             .body(pdfBytes);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/{contestId}/users/{userId}/roles")
    public ResponseEntity<?> getRolesUserNotApprovedInContest(
        @PathVariable String userId,
        @PathVariable String contestId
    ) {
        ModelGetRolesOfUserInContestResponse res = problemTestCaseService.getRolesOfUserInContest(userId, contestId);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contests/permissions")
    public ResponseEntity<?> getPermissionsOfMemberOfContest() {
        List<String> perms = UserRegistrationContestEntity.getListPermissions();
        return ResponseEntity.ok().body(perms);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/switch-judge-mode")
    public ResponseEntity<?> switchAllContestJudgeMode(@RequestParam("mode") String judgeMode) {

        problemTestCaseService.switchAllContestJudgeMode(judgeMode);

        return ResponseEntity.ok().body("ok");
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/students/upload-list-for-update-fullname")
    public ResponseEntity<?> uploadExcelStudentListForUpdateFullNameOfContest(
        @RequestParam("inputJson") String inputJson,
        @RequestParam("file") MultipartFile file
    ) {
        Gson gson = new Gson();
        ModelUploadExcelParticipantToContestInput modelUpload = gson.fromJson(
            inputJson, ModelUploadExcelParticipantToContestInput.class);
        List<ModelAddUserToContestResponse> uploadedUsers = new ArrayList<>();
        String contestId = modelUpload.getContestId();
        String role = modelUpload.getRole();
        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            //System.out.println("uploadExcelStudentListOfQuizTest, lastRowNum = " + lastRowNum);
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                Cell c = row.getCell(0);

                if (c == null || c.getStringCellValue().equals("")) {
                    continue;
                }
                String userId = c.getStringCellValue();
                c = row.getCell(1);

                if (c == null || c.getStringCellValue().equals("")) {
                    continue;
                }
                String fullname = c.getStringCellValue();
                ModelAddUserToContest m = new ModelAddUserToContest();
                m.setContestId(contestId);
                m.setUserId(userId);
                m.setFullname(fullname);
                if ("Manager".equalsIgnoreCase(role)) {
                    m.setRole(UserRegistrationContestEntity.ROLE_MANAGER);
                } else if ("Owner".equalsIgnoreCase(role)) {
                    m.setRole(UserRegistrationContestEntity.ROLE_OWNER);
                } else {
                    m.setRole(UserRegistrationContestEntity.ROLE_PARTICIPANT);
                }
                ModelAddUserToContestResponse response = problemTestCaseService.updateUserFullnameOfContest(m);
                uploadedUsers.add(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().body(uploadedUsers);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/students/upload-list")
    public ResponseEntity<?> uploadExcelStudentListOfContest(
        @RequestParam("inputJson") String inputJson,
        @RequestParam("file") MultipartFile file
    ) {
        Gson gson = new Gson();
        ModelUploadExcelParticipantToContestInput modelUpload = gson.fromJson(
            inputJson, ModelUploadExcelParticipantToContestInput.class);
        List<ModelAddUserToContestResponse> uploadedUsers = new ArrayList<>();
        String contestId = modelUpload.getContestId();
        String role = modelUpload.getRole();
        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            //System.out.println("uploadExcelStudentListOfQuizTest, lastRowNum = " + lastRowNum);
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                Cell c = row.getCell(0);

                if (c == null || c.getStringCellValue().equals("")) {
                    continue;
                }
                String userId = c.getStringCellValue();
                ModelAddUserToContest m = new ModelAddUserToContest();
                m.setContestId(contestId);
                m.setUserId(userId);
                if ("Manager".equalsIgnoreCase(role)) {
                    m.setRole(UserRegistrationContestEntity.ROLE_MANAGER);
                } else if ("Owner".equalsIgnoreCase(role)) {
                    m.setRole(UserRegistrationContestEntity.ROLE_OWNER);
                } else {
                    m.setRole(UserRegistrationContestEntity.ROLE_PARTICIPANT);
                }
                ModelAddUserToContestResponse response = problemTestCaseService.addUserToContest(m);
                uploadedUsers.add(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().body(uploadedUsers);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/contests/students/upload-group-list")
    public ResponseEntity<?> uploadExcelStudentGroupListOfContest(
        Principal principal,
        @RequestParam("inputJson") String inputJson,
        @RequestParam("file") MultipartFile file
    ) {
        String userLoginId = principal.getName();
        Gson gson = new Gson();
        ModelUploadExcelParticipantToContestInput modelUpload = gson.fromJson(
            inputJson, ModelUploadExcelParticipantToContestInput.class);
        List<ModelAddUserToContestGroupResponse> uploadedUsers = new ArrayList<>();
        String contestId = modelUpload.getContestId();
        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            //System.out.println("uploadExcelStudentListOfQuizTest, lastRowNum = " + lastRowNum);
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                Cell c = row.getCell(0);

                if (c == null || c.getStringCellValue().equals("")) {
                    continue;
                }
                String participantId = c.getStringCellValue();
                ModelAddUserToContestGroup m = new ModelAddUserToContestGroup();
                m.setContestId(contestId);
                m.setUserId(userLoginId);
                m.setParticipantId(participantId);

                ModelAddUserToContestGroupResponse response = problemTestCaseService.addUserToContestGroup(m);
                uploadedUsers.add(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().body(uploadedUsers);
    }

    @GetMapping("/contests/users/{userLoginId}/contest-result")
    public ResponseEntity<?> getContestResultOnProblemOfAUser(
        @PathVariable("userLoginId") String userLoginId
    ) {
        log.info("getContestResultOnProblemOfAUser, user = " + userLoginId);
        List<ContestSubmission> lst = problemTestCaseService.getNewestSubmissionResults(userLoginId);

        return ResponseEntity.ok().body(lst);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/contest/get-participant-view-submission-modes")
    public ResponseEntity<?> getParticipantViewSubmissionModes() {
        List<String> res = ContestEntity.getListParticipantViewSubmissionModes();
        return ResponseEntity.ok().body(res);
    }
}
