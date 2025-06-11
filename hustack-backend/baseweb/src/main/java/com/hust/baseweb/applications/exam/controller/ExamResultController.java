package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamResultUpdateReq;
import com.hust.baseweb.applications.exam.service.ExamResultService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/exam-result")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamResultController {

    ExamResultService examResultService;

    @Secured("ROLE_TEACHER")
    @PutMapping
    public ResponseEntity<ResponseData<ExamResultEntity>> update(@RequestBody @Valid ExamResultUpdateReq examResultUpdateReq) {
        return ResponseEntity.ok(examResultService.update(examResultUpdateReq));
    }
}
