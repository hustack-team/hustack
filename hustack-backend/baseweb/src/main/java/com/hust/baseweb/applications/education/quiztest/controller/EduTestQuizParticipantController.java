package com.hust.baseweb.applications.education.quiztest.controller;

import com.hust.baseweb.applications.education.quiztest.entity.EduTestQuizParticipant;
import com.hust.baseweb.applications.education.quiztest.model.edutestquizparticipation.EduTestQuizParticipationCreateInputModel;
import com.hust.baseweb.applications.education.quiztest.service.EduTestQuizParticipantService;
import com.hust.baseweb.entity.UserLogin;
import com.hust.baseweb.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
public class EduTestQuizParticipantController {

    EduTestQuizParticipantService eduTestQuizParticipantService;

    UserService userService;

    @PostMapping("/create-quiz-test-participation-register")
    public ResponseEntity<?> createQuizTestParticipationRegister(
        Principal principal, @RequestBody
    EduTestQuizParticipationCreateInputModel input
    ) {
        UserLogin u = userService.findById(principal.getName());
        log.info("createQuizTestParticipationRegister, userLoginId = " + u.getUserLoginId());
        EduTestQuizParticipant eduTestQuizParticipant = eduTestQuizParticipantService.register(u, input);
        return ResponseEntity.ok().body(eduTestQuizParticipant);
    }
}
