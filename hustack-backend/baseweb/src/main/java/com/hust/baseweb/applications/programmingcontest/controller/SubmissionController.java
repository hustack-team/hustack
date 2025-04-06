package com.hust.baseweb.applications.programmingcontest.controller;

import com.hust.baseweb.applications.programmingcontest.callexternalapi.model.LmsLogModelCreate;
import com.hust.baseweb.applications.programmingcontest.callexternalapi.service.ApiService;
import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import com.hust.baseweb.applications.programmingcontest.entity.ContestSubmissionComment;
import com.hust.baseweb.applications.programmingcontest.entity.ContestSubmissionEntity;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.ContestSubmissionRepo;
import com.hust.baseweb.applications.programmingcontest.service.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmissionController {


    ContestSubmissionService contestSubmissionService;

    ContestService contestService;

    ContestSubmissionCommentService commentService;

    ContestSubmissionCommentService contestSubmissionCommentService;

    ProblemTestCaseService problemTestCaseService;

    ContestSubmissionRepo contestSubmissionRepo;

    ApiService apiService;

    SubmissionService submissionService;

    @Secured("ROLE_TEACHER")
    @PostMapping("/teacher/submissions/{submissionId}/disable")
    public ResponseEntity<?> teacherDisableSubmission(Principal principal, @PathVariable UUID submissionId) {
        return ResponseEntity.ok().body(problemTestCaseService.teacherDisableSubmission(
            principal.getName(),
            submissionId));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/teacher/submissions/{submissionId}/enable")
    public ResponseEntity<?> teacherEnableSubmission(Principal principal, @PathVariable UUID submissionId) {
        return ResponseEntity.ok().body(problemTestCaseService.teacherEnableSubmission(
            principal.getName(),
            submissionId));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher/submissions/{submissionId}")
    public ResponseEntity<?> getTestCasesResult(
        @PathVariable UUID submissionId
    ) {
        return ResponseEntity.ok().body(problemTestCaseService
                                            .getSubmissionDetailByTestcase(submissionId, null));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher/submissions/{submissionId}/testcases/{testcaseId}")
    public ResponseEntity<?> getTestCasesResultDetail(
        @PathVariable UUID submissionId, @PathVariable UUID testcaseId
    ) {
        return ResponseEntity.ok().body(problemTestCaseService
                                            .getSubmissionDetailByTestcase(submissionId, testcaseId));
    }

    @GetMapping("/student/submissions/{submissionId}")
    public ResponseEntity<?> getContestProblemSubmissionDetailByTestCaseOfASubmissionViewedByParticipant(
        Principal principal, @PathVariable UUID submissionId
    ) {
        List<SubmissionDetailByTestcaseOM> retLst;
        try {
            retLst = problemTestCaseService
                .getParticipantSubmissionDetailByTestCase(
                    principal.getName(),
                    submissionId);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
        return ResponseEntity.ok().body(retLst);
    }

    @GetMapping("/student/submissions/{submissionId}/general-info")
    public ResponseEntity<?> getContestSubmissionDetailViewedByParticipant(
        Principal principal,
        @PathVariable("submissionId")
        UUID submissionId
    ) {
        return ResponseEntity.ok().body(submissionService.findById(principal.getName(), submissionId));
    }

    @Async
    public void logTeacherViewDetailSubmissionOfStudentContest(
        String userId,
        String contestId,
        String problemId,
        String studentId,
        UUID submissionId
    ) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();
        logM.setUserId(userId);
        log.info("logTeacherViewDetailSubmissionOfStudentContest, userId = " + logM.getUserId());
        logM.setParam1(contestId);
        logM.setParam2(problemId);
        logM.setParam3(submissionId.toString());
        logM.setParam4(studentId);

        logM.setActionType("MANAGER_VIEW_DETAIL_A_SUBMISSION_OF_STUDENT_CONTEST");
        logM.setDescription("an user manager views detail of a submission of a student in a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }


    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher/submissions/{submissionId}/general-info")
    public ResponseEntity<?> getContestSubmissionDetailViewedByManager(
        Principal principal,
        @PathVariable("submissionId") UUID submissionId
    ) {
        ContestSubmissionEntity contestSubmission = problemTestCaseService.getContestSubmissionDetailForTeacher(
            submissionId);

        logTeacherViewDetailSubmissionOfStudentContest(
            principal.getName(),
            contestSubmission.getContestId(),
            contestSubmission.getProblemId(),
            contestSubmission.getUserId(),
            contestSubmission.getContestSubmissionId());

        return ResponseEntity.ok().body(contestSubmission);
    }

//    @Secured("ROLE_TEACHER")
//    @GetMapping("/teacher/submissions/{submissionId}/general-info")
//    public ResponseEntity<?> getContestSubmissionDetailViewedByManager(
//        @PathVariable("submissionId") UUID submissionId
//    ) {
//        ContestSubmissionEntity contestSubmission = problemTestCaseService.getContestSubmissionDetailForTeacher(
//            submissionId);
//        return ResponseEntity.ok().body(contestSubmission);
//    }

    @GetMapping("/submissions/{submissionId}/contest")
    public ResponseEntity<?> getContestInfosOfASubmission(@PathVariable("submissionId") UUID submissionId) {
        ModelGetContestInfosOfSubmissionOutput res = problemTestCaseService.getContestInfosOfASubmission(submissionId);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @PutMapping("/submissions/source-code")
    public ResponseEntity<?> updateContestSubmissionSourceCode(
        @RequestBody ModelUpdateContestSubmission input
    ) {
        ContestSubmissionEntity sub = problemTestCaseService.updateContestSubmissionSourceCode(input);
        return ResponseEntity.ok().body(sub);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/submissions/{submissionId}/evaluation")
    public ResponseEntity<?> evaluateSubmission(@PathVariable UUID submissionId) {
        problemTestCaseService.evaluateSubmission(submissionId);
        return ResponseEntity.ok().body("ok");
    }

    @Secured("ROLE_TEACHER")
    //@PostMapping("/submissions/{contestId}/{problemId}/evaluation")
    //public ResponseEntity<?> evaluateSubmissionOfAProblemInContest(@PathVariable String contestId, @PathVariable String problemId) {
    @PostMapping("/submissions-of-a-problem-in-contest/rejudge")
    public ResponseEntity<?> evaluateSubmissionOfAProblemInContest(
        Principal principal,
        @RequestBody
        ModelInputRejudgeSubmissionsOfAProblemInContest m
    ) {
        log.info("evaluateSubmissionOfAProblemInContest, contestId = " +
                 m.getContestId() +
                 " and problemId = " +
                 m.getProblemId());
        problemTestCaseService.evaluateSubmissions(m.getContestId(), m.getProblemId());

        return ResponseEntity.ok().body("ok");
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/submissions/{contestId}/batch-evaluation")
    public ResponseEntity<?> evaluateBatchSubmissionContest(@PathVariable String contestId) {
        log.info("evaluateBatchSubmissionContest, contestId = {}", contestId);
        return ResponseEntity.ok().body(problemTestCaseService.reJudgeAllSubmissionsOfContest(contestId));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/submissions/{contestId}/batch-non-evaluated-evaluation")
    public ResponseEntity<?> evaluateBatchNotEvaluatedSubmissionContest(
        @PathVariable String contestId
    ) {
        log.info("evaluateBatchNotEvaluatedSubmissionContest, contestId = {}", contestId);
        return ResponseEntity.ok().body(submissionService.judgeAllSubmissionsOfContest(contestId));
    }

    @PostMapping("/submissions/{contestId}/batch-non-evaluated-evaluation-with-delay-time")
    public ResponseEntity<?> evaluateBatchNotEvaluatedSubmissionContestWithDelayTime(
        @PathVariable String contestId
    ) {
        log.info("evaluateBatchNotEvaluatedSubmissionContest, contestId = {}", contestId);
        return ResponseEntity.ok().body(submissionService.judgeAllSubmissionsOfContestWithDelayTime(contestId));
    }

//    @PostMapping("/submissions/testcases/solution-output")
//    public ResponseEntity<?> submitSolutionOutputOfATestCase(
//        Principal principale,
//        @RequestParam("inputJson") String inputJson,
//        @RequestParam("file") MultipartFile file
//    ) {
//        Gson gson = new Gson();
//        ModelSubmitSolutionOutputOfATestCase model = gson.fromJson(
//            inputJson,
//            ModelSubmitSolutionOutputOfATestCase.class);
//        try {
//            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
//            String solutionOutput = IOUtils.toString(stream, StandardCharsets.UTF_8);
//            stream.close();
//
//            ModelContestSubmissionResponse resp = problemTestCaseService.submitSolutionOutputOfATestCase(
//                principale.getName(),
//                solutionOutput,
//                model
//            );
//            log.info("resp {}", resp);
//            return ResponseEntity.ok().body(resp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ResponseEntity.ok().body("OK");
//
//    }
//
//    @PostMapping("/submissions/solution-output")
//    public ResponseEntity<?> submitSolutionOutput(
//        Principal principale,
//        @RequestParam("inputJson") String inputJson,
//        @RequestParam("file") MultipartFile file
//    ) {
//        log.info("submitSolutionOutput, inputJson = " + inputJson);
//        Gson gson = new Gson();
//        ModelSubmitSolutionOutput model = gson.fromJson(inputJson, ModelSubmitSolutionOutput.class);
//        try {
//            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
//            String solutionOutput = IOUtils.toString(stream, StandardCharsets.UTF_8);
//            stream.close();
//
//            ModelContestSubmissionResponse resp = problemTestCaseService.submitSolutionOutput(
//                solutionOutput,
//                model.getContestId(),
//                model.getProblemId(),
//                model.getTestCaseId(),
//                principale.getName());
//            return ResponseEntity.ok().body(resp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ResponseEntity.ok().body("OK");
//    }

    @Async
    public void logStudentSubmitToAContest(
        String userId, String contestId,
        ModelContestSubmitProgramViaUploadFile model
    ) {
        if (true) {
            return;
        }
        LmsLogModelCreate logM = new LmsLogModelCreate();

        logM.setUserId(userId);
        log.info("logStudentSubmitToAContest, userId = " + logM.getUserId());
        logM.setParam1(contestId);
        logM.setParam2(model.getProblemId());
        logM.setParam3(model.getLanguage());

        logM.setActionType("PARTICIPANT_SUBMIT_SOLUTION_CODE_TO_CONTEST");
        logM.setDescription("a participant submit solution code to a contest");
        apiService.callLogAPI("https://analytics.soict.ai/api/log/create-log", logM);
    }


    @PostMapping(value = "/submissions/file-upload",
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> contestSubmitProblemViaUploadFileV3(
        Principal principal,
        @RequestPart("dto") ModelContestSubmitProgramViaUploadFile model,
        @RequestPart(value = "file") MultipartFile file
    ) {
        logStudentSubmitToAContest(principal.getName(), model.getContestId(), model);
        return ResponseEntity.ok().body(submissionService.submitSubmission(principal.getName(), model, file));
    }


    @Secured("ROLE_TEACHER")
    @PostMapping(value = "/teacher/submissions/participant-code",
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> ManagerSubmitCodeOfParticipant(
        Principal principal,
        @RequestPart("dto") ModelInputManagerSubmitCodeOfParticipant dto,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok().body(submissionService.managerSubmitCodeOfParticipant(principal, dto, file));
    }

    @GetMapping("/submissions/users/{userLoginId}")
    public ResponseEntity<?> getContestSubmissionPagingOfAUser(
        @PathVariable("userLoginId") String userLoginId,
        Pageable pageable
    ) {
        log.info("getContestSubmissionPagingOfAUser, user = " + userLoginId);
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        Page<ContestSubmission> page = problemTestCaseService.findContestSubmissionByUserLoginIdPaging(
            pageable,
            userLoginId);
        log.info("page {}", page);
        return ResponseEntity.ok().body(page);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/teacher/submissions/{submissionId}/comments")
    public ResponseEntity<?> postComment(
        @PathVariable UUID submissionId,
        @RequestBody @Valid ModelContestSubmissionComment modelContestSubmissionComment,
        Principal principal
    ) throws Exception {
        log.info("postComment for submissionId {}: {}", submissionId, modelContestSubmissionComment);

        ContestSubmissionComment comment = contestSubmissionCommentService.postComment(
            submissionId,
            modelContestSubmissionComment,
            principal.getName()
        );

        return ResponseEntity.ok().body(comment);
    }

    @GetMapping("/submissions/{submissionId}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable UUID submissionId) {
        ContestSubmissionEntity submission = contestSubmissionService.getSubmissionById(submissionId);

        String contestId = submission.getContestId();

        ContestEntity contest = contestService.findContest(contestId);

        if (!"Y".equals(contest.getContestShowComment())) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<CommentDTO> comments = commentService.getAllCommentsBySubmissionId(submissionId);

        Collections.reverse(comments);

        return ResponseEntity.ok(comments);
    }

//    @Secured("ROLE_TEACHER")
//    @PutMapping("/teacher/submissions/{submissionId}/comments/{commentId}")
//    public ResponseEntity<?> updateComment(
//        @PathVariable UUID submissionId,
//        @PathVariable UUID commentId,
//        @RequestBody ContestSubmissionComment updatedComment
//    ) {
//        ContestSubmissionComment existingComment = contestSubmissionCommentRepo.findById(commentId)
//                                                                               .orElseThrow(() -> new RuntimeException("Comment not found"));
//
//        existingComment.setComment(updatedComment.getComment());
//        existingComment.setLastUpdatedStamp(new Date());
//
//        ContestSubmissionComment savedComment = contestSubmissionCommentRepo.save(existingComment);
//        return ResponseEntity.ok().body(savedComment);
//    }
}
