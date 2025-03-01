package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.*;
import com.hust.baseweb.applications.exam.service.ExamTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/exam-tag")
@RequiredArgsConstructor
public class ExamTagController {

    private final ExamTagService examTagService;

    @Secured("ROLE_TEACHER")
    @GetMapping("/get-all")
    public ResponseEntity<List<ExamTagEntity>> getAll() {
        return ResponseEntity.ok(examTagService.getAll());
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/create")
    public ResponseEntity<ResponseData<ExamTagEntity>> create(@RequestBody @Valid ExamTagSaveReq examTagSaveReq) {
        return ResponseEntity.ok(examTagService.create(examTagSaveReq));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/update")
    public ResponseEntity<ResponseData<ExamTagEntity>> update(@RequestBody @Valid ExamTagSaveReq examTagSaveReq) {
        return ResponseEntity.ok(examTagService.update(examTagSaveReq));
    }
}
