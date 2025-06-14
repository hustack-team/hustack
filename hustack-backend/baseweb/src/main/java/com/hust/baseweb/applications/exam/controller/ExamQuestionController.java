package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.entity.ExamQuestionEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamQuestionFilterReq;
import com.hust.baseweb.applications.exam.model.request.ExamQuestionSaveReq;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionFilterRes;
import com.hust.baseweb.applications.exam.service.ExamQuestionService;
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
import org.springframework.web.multipart.MultipartFile;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@RequestMapping("/exam-question")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamQuestionController {

    ExamQuestionService examQuestionService;

    @Secured("ROLE_TEACHER")
    @GetMapping
    public ResponseEntity<Page<ExamQuestionFilterRes>> filter(Pageable pageable, ExamQuestionFilterReq examQuestionFilterReq) {
        return ResponseEntity.ok(examQuestionService.filter(pageable, examQuestionFilterReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<ExamQuestionDetailsRes>> details(@PathVariable("id") String id) {
        return ResponseEntity.ok(examQuestionService.details(id));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping
    public ResponseEntity<ResponseData<ExamQuestionEntity>> create(@RequestPart("body") ExamQuestionSaveReq examQuestionSaveReq,
                                                                   @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(examQuestionService.create(examQuestionSaveReq, files));
    }

    @Secured("ROLE_TEACHER")
    @PutMapping
    public ResponseEntity<ResponseData<ExamQuestionEntity>> update(@RequestPart("body") ExamQuestionSaveReq examQuestionSaveReq,
                                                                   @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseEntity.ok(examQuestionService.update(examQuestionSaveReq, files));
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<ExamQuestionEntity>> delete(@PathVariable("id") String id) {
        return ResponseEntity.ok(examQuestionService.delete(id));
    }
}
