package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamEntity;
import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.*;
import com.hust.baseweb.applications.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamController {

    ExamService examService;

    @Secured("ROLE_TEACHER")
    @GetMapping
    public ResponseEntity<Page<ExamEntity>> filter(
        Pageable pageable, ExamFilterReq examFilterReq) {
        return ResponseEntity.ok(examService.filter(pageable, examFilterReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<ExamDetailsRes>> details(@PathVariable("id") String id) {
        return ResponseEntity.ok(examService.details(id));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/examTest/{examExamTestId}")
    public ResponseEntity<ResponseData<List<ExamStudentResultDetailsRes>>> detailStudentExam(@PathVariable("examExamTestId") String examExamTestId) {
        return ResponseEntity.ok(examService.detailStudentExam(examExamTestId));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/teacher/submissions/{examStudentTestId}")
    public ResponseEntity<ResponseData<ExamMarkingDetailsRes>> detailsExamMarking(@PathVariable String examStudentTestId) {
        return ResponseEntity.ok(examService.detailsExamMarking(examStudentTestId));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/teacher/submissions")
    public ResponseEntity<ResponseData<ExamResultEntity>> markingExam(@RequestPart("body") ExamMarkingSaveReq examMarkingSaveReq,
                                                                      @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(examService.markingExam(examMarkingSaveReq, files));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamEntity>> create(@RequestBody @Valid ExamSaveReq examSaveReq) {
        return ResponseEntity.ok(examService.create(examSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/preview-update/{id}")
    public ResponseEntity<ResponseData<ExamPreviewUpdateRes>> previewUpdate(@PathVariable("id") String id) {
        return ResponseEntity.ok(examService.previewUpdate(id));
    }

    @Secured("ROLE_TEACHER")
    @PutMapping
    public ResponseEntity<ResponseData<ExamEntity>> update(@RequestBody @Valid ExamSaveReq examSaveReq) {
        return ResponseEntity.ok(examService.update(examSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<ExamEntity>> delete(@PathVariable("id") String id) {
        return ResponseEntity.ok(examService.delete(id));
    }

    @GetMapping("/student/submissions")
    public ResponseEntity<Page<MyExamFilterRes>> filterMyExam(
        Pageable pageable, MyExamFilterReq myExamFilterReq) {
        return ResponseEntity.ok(examService.filterMyExam(pageable, myExamFilterReq));
    }

    @GetMapping("/student/submissions/examTest/{examTestIds}")
    public ResponseEntity<ResponseData<List<MyExamTestWithResultRes>>> getListTestMyExam(@PathVariable("examTestIds") String examTestIds) {
        return ResponseEntity.ok(examService.getListTestMyExam(examTestIds));
    }

    @GetMapping("/student/submissions/examStudentTest/{examStudentTestId}")
    public ResponseEntity<ResponseData<MyExamDetailsRes>> detailsMyExam(@PathVariable("examStudentTestId") String examStudentTestId) {
        return ResponseEntity.ok(examService.detailsMyExam(examStudentTestId));
    }

    @PostMapping("/student/submissions")
    public ResponseEntity<ResponseData<ExamResultEntity>> doingMyExam(@RequestPart("body") MyExamResultSaveReq myExamResultSaveReq,
                                                                      @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(examService.doingMyExam(myExamResultSaveReq, files));
    }
}
