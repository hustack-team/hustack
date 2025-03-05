package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamEntity;
import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.model.response.ExamDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamMarkingDetailsRes;
import com.hust.baseweb.applications.exam.model.response.MyExamDetailsRes;
import com.hust.baseweb.applications.exam.model.response.MyExamFilterRes;
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
    @GetMapping("/teacher/submissions/{examStudentId}")
    public ResponseEntity<ResponseData<ExamMarkingDetailsRes>> detailsExamMarking(@PathVariable String examStudentId) {
        return ResponseEntity.ok(examService.detailsExamMarking(examStudentId));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/teacher/submissions")
    public ResponseEntity<ResponseData<ExamResultEntity>> markingExam(@RequestBody ExamMarkingSaveReq examMarkingSaveReq) {
        return ResponseEntity.ok(examService.markingExam(examMarkingSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamEntity>> create(@RequestBody @Valid ExamSaveReq examSaveReq) {
        return ResponseEntity.ok(examService.create(examSaveReq));
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

    @GetMapping("/student/submissions/{examId}/{examStudentId}")
    public ResponseEntity<ResponseData<MyExamDetailsRes>> detailsMyExam(@PathVariable("examId") String examId,
                                                                        @PathVariable("examStudentId") String examStudentId) {
        return ResponseEntity.ok(examService.detailsMyExam(examId, examStudentId));
    }

    @PostMapping("/student/submissions")
    public ResponseEntity<ResponseData<ExamResultEntity>> doingMyExam(@RequestPart("body") MyExamResultSaveReq myExamResultSaveReq,
                                                                      @RequestPart("files") MultipartFile[] files) {
        return ResponseEntity.ok(examService.doingMyExam(myExamResultSaveReq, files));
    }
}
