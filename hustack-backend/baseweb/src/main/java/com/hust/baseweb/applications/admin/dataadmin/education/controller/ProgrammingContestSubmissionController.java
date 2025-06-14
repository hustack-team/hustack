package com.hust.baseweb.applications.admin.dataadmin.education.controller;

import com.hust.baseweb.applications.admin.dataadmin.education.service.ProgrammingContestSubmissionServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Secured("ROLE_ADMIN")
@RequestMapping("/admin/data/programming-contests/submissions")
public class ProgrammingContestSubmissionController {

    ProgrammingContestSubmissionServiceImpl contestSubmissionService;

    @GetMapping("/{studentId}")
    public ResponseEntity<?> getContestSubmissionsOfStudent(
        @PathVariable("studentId") String studentLoginId,
        @RequestParam String search,
        @RequestParam int page,
        @RequestParam int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
            contestSubmissionService.findContestSubmissionsOfStudent(studentLoginId, search, pageable)
        );
    }
}
