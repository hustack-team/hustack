package com.hust.baseweb.applications.examclassandaccount.controller;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import com.hust.baseweb.applications.examclassandaccount.entity.RandomGeneratedUserLogin;
import com.hust.baseweb.applications.examclassandaccount.model.ExamClassAccountDTO;
import com.hust.baseweb.applications.examclassandaccount.model.ExamClassAccountStatusUpdateRequestDTO;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateGeneratedUserLogin;
import com.hust.baseweb.applications.examclassandaccount.repo.RandomGeneratedUserLoginRepo;
import com.hust.baseweb.applications.examclassandaccount.service.ExamClassService;
import com.hust.baseweb.applications.examclassandaccount.service.ExamClassUserloginMapService;
import com.hust.baseweb.dto.response.ApiResponse;
import com.hust.baseweb.dto.response.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ExamClassAccountController {

    ExamClassService examClassService;

    ExamClassUserloginMapService examClassUserloginMapService;

    RandomGeneratedUserLoginRepo randomGeneratedUserLoginRepo;

    @Secured("ROLE_ADMIN")
    @GetMapping("/exam-classes")
    public ResponseEntity<?> getAllExamClass(Principal principal) {
        List<ExamClass> res = examClassService.getAllExamClass();
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/exam-classes")
    public ResponseEntity<?> createExamClass(Principal principal, @RequestBody ModelCreateExamClass m) {
        ExamClass ec = examClassService.createExamClass(principal.getName(), m);
        return ResponseEntity.ok().body(ec);
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
        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            List<ExamClassAccountDTO> users = new ArrayList<>();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                int columns = row.getLastCellNum();

                String email = null;
                String studentCode = null;
                DataFormatter formatter = new DataFormatter();
                if (columns > 0) {
                    Cell c = row.getCell(0);
                    if (c != null) {
                        email = formatter.formatCellValue(c).trim();
                    }
                }
                if (columns > 1) {
                    Cell c = row.getCell(1);
                    if (c != null) {
                        if (c.getCellType() == CellType.NUMERIC) {
                            studentCode = String.valueOf((long) c.getNumericCellValue());  // không có phần thập phân
                        } else {
                            studentCode = formatter.formatCellValue(c).trim();
                        }
                    }
                }

                if (StringUtils.isAllBlank(email, studentCode)) {
                    continue;
                }

                String fullName = null;
                if (columns > 2) {
                    Cell c = row.getCell(2);
                    if (c != null) {
                        fullName = formatter.formatCellValue(c).trim();
                    }
                }

                ExamClassAccountDTO u = new ExamClassAccountDTO(email, studentCode, fullName);
                users.add(u);
            }

            List<ExamClassUserloginMap> res = examClassUserloginMapService.importAccounts(examClassId, users);
            return ResponseEntity.ok().body(res);
        }
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> clearAccountExamClass(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        return ResponseEntity.ok().body(examClassService.deleteAccounts(principal.getName(), examClassId));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/exam-classes/{examClassId}")
    public ResponseEntity<?> getExamClassDetail(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        return ResponseEntity.ok().body(examClassService.getExamClassDetail(principal.getName(), examClassId));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/exam-classes/{examClassId}/accounts/export")
    public ResponseEntity<?> exportExamClass(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        byte[] pdfBytes = examClassService.exportExamClass(principal.getName(), examClassId);
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + examClassId + ".pdf")
                             .contentType(MediaType.APPLICATION_PDF)
                             .body(pdfBytes);
    }

    @Secured("ROLE_TEACHER")
    @PatchMapping("/exam-classes/{examClassId}/accounts/reset-password")
    public ResponseEntity<?> resetPassword(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        return ResponseEntity.ok().body(examClassService.resetPassword(principal.getName(), examClassId));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> generateAccounts(
        Principal principal,
        @PathVariable UUID examClassId
    ) {
        return ResponseEntity.ok().body(examClassService.generateAccounts(principal.getName(), examClassId));
    }

    @Secured("ROLE_TEACHER")
    @PatchMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> updateStatus(
        Principal principal,
        @PathVariable UUID examClassId,
        @RequestBody ExamClassAccountStatusUpdateRequestDTO dto
    ) {
        return ResponseEntity
            .ok()
            .body(examClassService.updateStatus(principal.getName(), examClassId, dto.isEnabled()));
    }

    /**
     * Xóa một tài khoản cụ thể khỏi lớp thi.
     *
     * @param examClassId ID của lớp thi.
     * @param accountId   ID của tài khoản ngẫu nhiên (randomUserLoginId).
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
     * Reset mật khẩu cho một tài khoản cụ thể.
     *
     * @param examClassId ID của lớp thi.
     * @param accountId   ID của tài khoản ngẫu nhiên (randomUserLoginId).
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
     * Cập nhật trạng thái (kích hoạt/vô hiệu hóa) cho một tài khoản cụ thể.
     *
     * @param examClassId ID của lớp thi.
     * @param accountId   ID của tài khoản ngẫu nhiên (randomUserLoginId).
     * @param dto         Đối tượng chứa trạng thái mới.
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
}
