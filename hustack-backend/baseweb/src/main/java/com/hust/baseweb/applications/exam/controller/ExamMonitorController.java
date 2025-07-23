package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamMonitorEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamMonitorSaveReq;
import com.hust.baseweb.applications.exam.service.ExamMonitorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/exam-monitor")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMonitorController {

    ExamMonitorService examMonitorService;

    @GetMapping("/result/{examResultId}")
    public ResponseEntity<List<ExamMonitorEntity>> getAll(@PathVariable("examResultId") String examResultId) {
        return ResponseEntity.ok(examMonitorService.findAllByExamResultId(examResultId));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamMonitorEntity>> create(@RequestBody List<ExamMonitorSaveReq> list) {
        return ResponseEntity.ok(examMonitorService.create(list));
    }
//
//    @Secured("ROLE_TEACHER")
//    @PutMapping
//    public ResponseEntity<ResponseData<ExamTagEntity>> update(@RequestBody @Valid ExamTagSaveReq examTagSaveReq) {
//        return ResponseEntity.ok(examTagService.update(examTagSaveReq));
//    }
}
