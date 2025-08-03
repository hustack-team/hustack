package com.hust.baseweb.applications.examclassandaccount.controller;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamAccount;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.RandomGeneratedUserLogin;
import com.hust.baseweb.applications.examclassandaccount.model.ExamClassAccountStatusUpdateRequestDTO;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateGeneratedUserLogin;
import com.hust.baseweb.applications.examclassandaccount.repo.RandomGeneratedUserLoginRepo;
import com.hust.baseweb.applications.examclassandaccount.service.ExamClassService;
import com.hust.baseweb.dto.response.ApiResponse;
import com.hust.baseweb.dto.response.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ExamClassAccountController {

    ExamClassService examClassService;

    RandomGeneratedUserLoginRepo randomGeneratedUserLoginRepo;

    @Secured("ROLE_ADMIN")
    @GetMapping("/exam-classes")
    public ResponseEntity<?> getAllExamClass(Principal principal) {
        List<ExamClass> res = examClassService.getAllExamClass();
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/exam-classes")
    public ResponseEntity<?> createExamClass(Principal principal, @RequestBody ModelCreateExamClass dto) {
        return ResponseEntity.ok().body(examClassService.createExamClass(principal.getName(), dto));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/create-a-generated-userlogin")
    public ResponseEntity<?> createAGeneratedUserLogin(
        Principal principal,
        @RequestBody ModelCreateGeneratedUserLogin m
    ) {
        RandomGeneratedUserLogin u = new RandomGeneratedUserLogin(
            m.getUserLoginId(),
            m.getPassword(),
            RandomGeneratedUserLogin.STATUS_ACTIVE);
        u = randomGeneratedUserLoginRepo.save(u);
        log.info("createAGeneratedUserLogin, created generated user " + u.getUserLoginId() + "," + u.getPassword());
        return ResponseEntity.ok().body(u);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping(value = "/exam-classes/{examClassId}/accounts/import",
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createExamAccountsMap(
        @PathVariable UUID examClassId,
        @RequestPart("file") MultipartFile file
    ) throws IOException {
        List<ExamAccount> res = examClassService.importAccountsFromExcel(examClassId, file);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> clearAccountExamClass(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        try {
            return ResponseEntity.ok().body(examClassService.deleteAccounts(principal.getName(), examClassId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(ApiResponse.of(ErrorCode.EXAM_CLASS_NOT_FOUND));
        }
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/exam-classes/{examClassId}")
    public ResponseEntity<?> getExamClassDetail(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        try {
            return ResponseEntity.ok(examClassService.getExamClassDetail(principal.getName(), examClassId));
        } catch (EntityNotFoundException e) {
            log.error("Exam class not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(ApiResponse.of(ErrorCode.EXAM_CLASS_NOT_FOUND));
        }
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/exam-classes/{examClassId}/accounts/export")
    public ResponseEntity<?> exportExamClass(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        try {
            byte[] pdfBytes = examClassService.exportExamClass(principal.getName(), examClassId);
            return ResponseEntity.ok()
                                 .header(
                                     HttpHeaders.CONTENT_DISPOSITION,
                                     "attachment; filename=" + examClassId + ".pdf")
                                 .contentType(MediaType.APPLICATION_PDF)
                                 .body(pdfBytes);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(ApiResponse.of(ErrorCode.EXAM_CLASS_NOT_FOUND));
        }
    }

    @Secured("ROLE_TEACHER")
    @PatchMapping("/exam-classes/{examClassId}/accounts/reset-password")
    public ResponseEntity<?> resetPassword(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        try {
            return ResponseEntity.ok().body(examClassService.resetPassword(principal.getName(), examClassId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(ApiResponse.of(ErrorCode.EXAM_CLASS_NOT_FOUND));
        }
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> generateAccounts(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        try {
            return ResponseEntity.ok().body(examClassService.generateAccounts(principal.getName(), examClassId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(ApiResponse.of(ErrorCode.EXAM_CLASS_NOT_FOUND));
        }
    }

    @Secured("ROLE_TEACHER")
    @PatchMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> updateStatus(
        Principal principal,
        @PathVariable UUID examClassId,
        @RequestBody ExamClassAccountStatusUpdateRequestDTO dto
    ) {
        try {
            return ResponseEntity
                .ok()
                .body(examClassService.updateStatus(principal.getName(), examClassId, dto.isEnabled()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(ApiResponse.of(ErrorCode.EXAM_CLASS_NOT_FOUND));
        }
    }

    /**
     * Delete a specific account from the exam class.
     *
     * @param examClassId ID of the exam class.
     * @param accountId   ID of the random user login (randomUserLoginId).
     */
    @Secured("ROLE_TEACHER")
    @DeleteMapping("/exam-classes/{examClassId}/accounts/{accountId}")
    public ResponseEntity<?> deleteAccount(
        Principal principal,
        @PathVariable UUID examClassId,
        @PathVariable UUID accountId
    ) {
        try {
            return ResponseEntity
                .ok()
                .body(examClassService.deleteAccount(principal.getName(), examClassId, accountId));
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.of(ErrorCode.ACCOUNT_NOT_FOUND_IN_EXAM_CLASS));
        }
    }

    /**
     * Reset password for a specific account.
     *
     * @param examClassId ID of the exam class.
     * @param accountId   ID of the random user login (randomUserLoginId).
     */
    @Secured("ROLE_TEACHER")
    @PatchMapping("/exam-classes/{examClassId}/accounts/{accountId}/reset-password")
    public ResponseEntity<?> resetPasswordForAccount(
        Principal principal,
        @PathVariable UUID examClassId,
        @PathVariable UUID accountId
    ) {
        try {
            return ResponseEntity.ok().body(examClassService.resetPasswordForAccount(
                principal.getName(),
                examClassId,
                accountId));
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.of(ErrorCode.ACCOUNT_NOT_FOUND_IN_EXAM_CLASS));
        }
    }

    /**
     * Update status (activate/deactivate) for a specific account.
     *
     * @param examClassId ID of the exam class.
     * @param accountId   ID of the random user login (randomUserLoginId).
     * @param dto         Object containing the new status.
     */
    @Secured("ROLE_TEACHER")
    @PatchMapping("/exam-classes/{examClassId}/accounts/{accountId}/status")
    public ResponseEntity<?> updateStatusForAccount(
        Principal principal,
        @PathVariable UUID examClassId,
        @PathVariable UUID accountId,
        @RequestBody ExamClassAccountStatusUpdateRequestDTO dto
    ) {
        try {
            return ResponseEntity.ok().body(examClassService.updateStatusForAccount(
                principal.getName(),
                examClassId,
                accountId,
                dto.isEnabled()));
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.of(ErrorCode.ACCOUNT_NOT_FOUND_IN_EXAM_CLASS));
        }
    }

    /**
     * Generate account for a specific accountId.
     *
     * @param examClassId ID of the exam class.
     * @param accountId   ID of the account to generate.
     */
    @Secured("ROLE_TEACHER")
    @PostMapping("/exam-classes/{examClassId}/accounts/{accountId}")
    public ResponseEntity<?> generateAccountForSingle(
        Principal principal,
        @PathVariable UUID examClassId,
        @PathVariable UUID accountId
    ) {
        try {
            return ResponseEntity.ok().body(examClassService.generateAccountForSingle(
                principal.getName(),
                examClassId,
                accountId));
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.of(ErrorCode.ACCOUNT_NOT_FOUND_IN_EXAM_CLASS));
        }
    }
}
