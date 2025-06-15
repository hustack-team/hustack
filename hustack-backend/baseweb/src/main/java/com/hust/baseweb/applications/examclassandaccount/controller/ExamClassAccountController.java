package com.hust.baseweb.applications.examclassandaccount.controller;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import com.hust.baseweb.applications.examclassandaccount.entity.RandomGeneratedUserLogin;
import com.hust.baseweb.applications.examclassandaccount.model.*;
import com.hust.baseweb.applications.examclassandaccount.repo.RandomGeneratedUserLoginRepo;
import com.hust.baseweb.applications.examclassandaccount.service.ExamClassService;
import com.hust.baseweb.applications.examclassandaccount.service.ExamClassUserloginMapService;
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
    @GetMapping("/get-all-exam-class")
    public ResponseEntity<?> getAllExamClass(Principal principal) {
        List<ExamClass> res = examClassService.getAllExamClass();
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/create-exam-class")
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

    @Secured("ROLE_ADMIN")
    @PostMapping(value = "/create-exam-accounts-map",
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createExamAccountsMap(
        @RequestPart("dto") ModelCreateExamAccountMap dto,
        @RequestPart("file") MultipartFile file
    ) throws IOException {
        UUID examClassId = dto.getExamClassId();

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

            List<ExamClassUserloginMap> res = examClassUserloginMapService.createExamClassAccount(examClassId, users);
            return ResponseEntity.ok().body(res);
        }
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> clearAccountExamClass(@PathVariable UUID examClassId) {
        return ResponseEntity.ok().body(examClassService.deleteAccounts(examClassId));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/get-exam-class-detail/{examClassId}")
    public ResponseEntity<?> getExamClassDetail(Principal principal, @PathVariable UUID examClassId) {
        ModelRepsonseExamClassDetail res = examClassService.getExamClassDetail(examClassId);
        return ResponseEntity.ok().body(res);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/exam-class/{examClassId}/export")
    public ResponseEntity<?> exportExamClass(@PathVariable UUID examClassId) {
        byte[] pdfBytes = examClassService.exportExamClass(examClassId);
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + examClassId + ".pdf")
                             .contentType(MediaType.APPLICATION_PDF)
                             .body(pdfBytes);
    }

    @Secured("ROLE_ADMIN")
    @PatchMapping("/exam-classes/{examClassId}/accounts/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable UUID examClassId) {
        return ResponseEntity.ok().body(examClassService.resetPassword(examClassId));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> generateAccounts(@PathVariable UUID examClassId) {
        return ResponseEntity.ok().body(examClassService.generateAccounts(examClassId));
    }

    @Secured("ROLE_ADMIN")
    @PatchMapping("/exam-classes/{examClassId}/accounts")
    public ResponseEntity<?> updateStatus(
        @PathVariable UUID examClassId,
        @RequestBody ExamClassAccountStatusUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok().body(examClassService.updateStatus(examClassId, dto.isEnabled()));
    }
}
