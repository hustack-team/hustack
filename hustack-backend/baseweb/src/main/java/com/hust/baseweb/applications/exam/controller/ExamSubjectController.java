package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamSubjectEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.service.ExamSubjectService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/exam-subject")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamSubjectController {

    ExamSubjectService examSubjectService;

    @Secured("ROLE_TEACHER")
    @GetMapping
    public ResponseEntity<Page<ExamSubjectEntity>> filter(
        Pageable pageable, ExamSubjectFilterReq examSubjectFilterReq) {
        return ResponseEntity.ok(examSubjectService.filter(pageable, examSubjectFilterReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/all")
    public ResponseEntity<List<ExamSubjectEntity>> getAll() {
        return ResponseEntity.ok(examSubjectService.getAll());
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamSubjectEntity>> create(@RequestBody @Valid ExamSubjectSaveReq examSubjectSaveReq) {
        return ResponseEntity.ok(examSubjectService.create(examSubjectSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PutMapping
    public ResponseEntity<ResponseData<ExamSubjectEntity>> update(@RequestBody @Valid ExamSubjectSaveReq examSubjectSaveReq) {
        return ResponseEntity.ok(examSubjectService.update(examSubjectSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<ExamSubjectEntity>> delete(@PathVariable("id") String id) {
        return ResponseEntity.ok(examSubjectService.delete(id));
    }
}
