package com.hust.baseweb.applications.admin.dataadmin.education.controller;

import com.hust.baseweb.applications.admin.dataadmin.education.service.DoingPracticeQuizLogsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/data/education/doing-practice-quiz-logs")
public class DoingPracticeQuizLogsController {

    DoingPracticeQuizLogsService doingPracticeQuizLogsService;

    @GetMapping("/{studentLoginId}")
    public ResponseEntity<?> getDoingPracticeQuizLogsOfStudent(
        @PathVariable String studentLoginId,
        @RequestParam String search,
        @RequestParam int page,
        @RequestParam int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
            doingPracticeQuizLogsService.findDoingPracticeQuizLogsOfStudent(
                studentLoginId, search, pageable
            )
        );
    }
}
