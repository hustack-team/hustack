package com.hust.baseweb.applications.exam.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hust.baseweb.applications.exam.entity.ExamQuestionEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamQuestionDeleteReq;
import com.hust.baseweb.applications.exam.model.request.ExamQuestionDetailsReq;
import com.hust.baseweb.applications.exam.model.request.ExamQuestionFilterReq;
import com.hust.baseweb.applications.exam.model.request.ExamQuestionSaveReq;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionFilterRes;
import com.hust.baseweb.applications.exam.service.ExamQuestionService;
import com.hust.baseweb.applications.exam.utils.LocalDateTimeAdapter;
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

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/exam-question")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamQuestionController {

    ExamQuestionService examQuestionService;

    @Secured("ROLE_TEACHER")
    @PostMapping("/filter")
    public ResponseEntity<Page<ExamQuestionFilterRes>> filter(Pageable pageable, @RequestBody ExamQuestionFilterReq examQuestionFilterReq) {
        return ResponseEntity.ok(examQuestionService.filter(pageable, examQuestionFilterReq));
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/details")
    public ResponseEntity<ResponseData<ExamQuestionDetailsRes>> details(@ModelAttribute ExamQuestionDetailsReq examQuestionDetailsReq) {
        return ResponseEntity.ok(examQuestionService.details(examQuestionDetailsReq));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/create")
    public ResponseEntity<ResponseData<ExamQuestionEntity>> create(@RequestParam("body") String questions,
                                                                   @RequestParam("files") MultipartFile[] files) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        return ResponseEntity.ok(examQuestionService.create(gson.fromJson(questions, ExamQuestionSaveReq.class), files));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/update")
    public ResponseEntity<ResponseData<ExamQuestionEntity>> update(@RequestParam("body") String questions,
                                                                   @RequestParam("files") MultipartFile[] files) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        return ResponseEntity.ok(examQuestionService.update(gson.fromJson(questions, ExamQuestionSaveReq.class), files));
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/delete")
    public ResponseEntity<ResponseData<ExamQuestionEntity>> delete(@RequestBody @Valid ExamQuestionDeleteReq examQuestionDeleteReq) {
        return ResponseEntity.ok(examQuestionService.delete(examQuestionDeleteReq));
    }
}
