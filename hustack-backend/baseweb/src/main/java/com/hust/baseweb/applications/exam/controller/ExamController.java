package com.hust.baseweb.applications.exam.controller;

import com.google.gson.Gson;
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
import lombok.RequiredArgsConstructor;
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
public class ExamController {

    private final ExamService examService;

    @Secured("ROLE_TEACHER")
    @GetMapping("/filter")
    public ResponseEntity<Page<ExamEntity>> filter(
        Pageable pageable, @ModelAttribute ExamFilterReq examFilterReq) {
        return ResponseEntity.ok(examService.filter(pageable, examFilterReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/details")
    public ResponseEntity<ResponseData<ExamDetailsRes>> details(@ModelAttribute ExamDetailsReq examDetailsReq) {
        return ResponseEntity.ok(examService.details(examDetailsReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/details-marking/{examStudentId}")
    public ResponseEntity<ResponseData<ExamMarkingDetailsRes>> detailsExamMarking(@PathVariable String examStudentId) {
        return ResponseEntity.ok(examService.detailsExamMarking(examStudentId));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/marking-exam")
    public ResponseEntity<ResponseData<ExamResultEntity>> markingExam(@RequestBody ExamMarkingSaveReq examMarkingSaveReq) {
        return ResponseEntity.ok(examService.markingExam(examMarkingSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/create")
    public ResponseEntity<ResponseData<ExamEntity>> create(@RequestBody @Valid ExamSaveReq examSaveReq) {
        return ResponseEntity.ok(examService.create(examSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/update")
    public ResponseEntity<ResponseData<ExamEntity>> update(@RequestBody @Valid ExamSaveReq examSaveReq) {
        return ResponseEntity.ok(examService.update(examSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/delete")
    public ResponseEntity<ResponseData<ExamEntity>> delete(@RequestBody @Valid ExamDeleteReq examDeleteReq) {
        return ResponseEntity.ok(examService.delete(examDeleteReq));
    }

    @GetMapping("/filter-my-exam")
    public ResponseEntity<Page<MyExamFilterRes>> filter(
        Pageable pageable, @ModelAttribute MyExamFilterReq myExamFilterReq) {
        return ResponseEntity.ok(examService.filterMyExam(pageable, myExamFilterReq));
    }

    @GetMapping("/details-my-exam")
    public ResponseEntity<ResponseData<MyExamDetailsRes>> detailsMyExam(@ModelAttribute MyExamDetailsReq myExamDetailsReq) {
        return ResponseEntity.ok(examService.detailsMyExam(myExamDetailsReq));
    }

    @PostMapping("/doing-my-exam")
    public ResponseEntity<ResponseData<ExamResultEntity>> doingMyExam(@RequestParam("body") String body,
                                                                      @RequestParam("files") MultipartFile[] files) {
        Gson gson = new Gson();
        return ResponseEntity.ok(examService.doingMyExam(gson.fromJson(body, MyExamResultSaveReq.class), files));
    }
}
