package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamTagSaveReq;
import com.hust.baseweb.applications.exam.service.ExamTagService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@RequestMapping("/exam-tag")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamTagController {

    ExamTagService examTagService;

    @Secured("ROLE_TEACHER")
    @GetMapping
    public ResponseEntity<List<ExamTagEntity>> getAll() {
        return ResponseEntity.ok(examTagService.getAll());
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamTagEntity>> create(@RequestBody @Valid ExamTagSaveReq examTagSaveReq) {
        return ResponseEntity.ok(examTagService.create(examTagSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PutMapping
    public ResponseEntity<ResponseData<ExamTagEntity>> update(@RequestBody @Valid ExamTagSaveReq examTagSaveReq) {
        return ResponseEntity.ok(examTagService.update(examTagSaveReq));
    }
}
