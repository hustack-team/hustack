package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamTestEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamTestFilterReq;
import com.hust.baseweb.applications.exam.model.request.ExamTestSaveReq;
import com.hust.baseweb.applications.exam.model.response.ExamTestDetailsRes;
import com.hust.baseweb.applications.exam.service.ExamTestService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@RequestMapping("/exam-test")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamTestController {

    ExamTestService examTestService;

    @Secured("ROLE_TEACHER")
    @GetMapping
    public ResponseEntity<Page<ExamTestEntity>> filter(Pageable pageable, ExamTestFilterReq examTestFilterReq) {
        return ResponseEntity.ok(examTestService.filter(pageable, examTestFilterReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<ExamTestDetailsRes>> details(@PathVariable("id") String id) {
        return ResponseEntity.ok(examTestService.details(id));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamTestEntity>> create(@RequestBody @Valid ExamTestSaveReq examTestSaveReq) {
        return ResponseEntity.ok(examTestService.create(examTestSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PutMapping
    public ResponseEntity<ResponseData<ExamTestEntity>> update(@RequestBody @Valid ExamTestSaveReq examTestSaveReq) {
        return ResponseEntity.ok(examTestService.update(examTestSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<ExamTestEntity>> delete(@PathVariable("id") String id) {
        return ResponseEntity.ok(examTestService.delete(id));
    }
}
