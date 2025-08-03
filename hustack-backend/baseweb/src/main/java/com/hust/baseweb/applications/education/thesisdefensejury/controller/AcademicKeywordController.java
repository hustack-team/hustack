package com.hust.baseweb.applications.education.thesisdefensejury.controller;

import com.hust.baseweb.applications.education.thesisdefensejury.entity.AcademicKeyword;
import com.hust.baseweb.applications.education.thesisdefensejury.service.AcademicKeywordService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicKeywordController {

    AcademicKeywordService academicKeywordService;

    @GetMapping("/academic_keywords")
    public ResponseEntity<?> getThesis(Pageable pageable) {
        List<AcademicKeyword> res = academicKeywordService.getAllAcademicKeywords();
        return ResponseEntity.ok().body(res);
    }
}
