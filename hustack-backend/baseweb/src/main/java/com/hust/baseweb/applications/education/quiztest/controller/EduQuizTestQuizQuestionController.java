package com.hust.baseweb.applications.education.quiztest.controller;

import com.hust.baseweb.applications.education.model.quiz.QuizQuestionDetailModel;
import com.hust.baseweb.applications.education.quiztest.entity.EduQuizTestQuizQuestion;
import com.hust.baseweb.applications.education.quiztest.model.EduQuizTestModel;
import com.hust.baseweb.applications.education.quiztest.model.quiztestquestion.CreateQuizTestQuestionInputModel;
import com.hust.baseweb.applications.education.quiztest.service.EduQuizTestQuizQuestionService;
import com.hust.baseweb.entity.UserLogin;
import com.hust.baseweb.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-module-quiz-test",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Secured("ROLE_TEACHER")
public class EduQuizTestQuizQuestionController {

    EduQuizTestQuizQuestionService eduQuizTestQuizQuestionService;

    UserService userService;

    @GetMapping("/get-quiz-test-using-question/{questionId}")
    public ResponseEntity<?> getQuizTestsUsingQuestion(Principal principal, @PathVariable UUID questionId) {
        List<EduQuizTestModel> eduQuizTestModels = eduQuizTestQuizQuestionService.getQuizTestsUsingQuestion(questionId);
        return ResponseEntity.ok().body(eduQuizTestModels);
    }

    @GetMapping("/get-questions-of-quiz-test/{testId}")
    public ResponseEntity<?> getQuestionOfQuizTest(Principal principal, @PathVariable String testId) {
        log.info("getQuestionOfQuizTest, testId = " + testId);
        List<QuizQuestionDetailModel> eduQuizTestQuizQuestionList =
            eduQuizTestQuizQuestionService.findAllByTestId(testId);
        return ResponseEntity.ok().body(eduQuizTestQuizQuestionList);
    }

    @PostMapping("/add-question-to-quiz-test")
    public ResponseEntity<?> addQuestionToQuizTest(
        Principal principal,
        @RequestBody CreateQuizTestQuestionInputModel input
    ) {
        UserLogin u = userService.findById(principal.getName());
        log.info("addQuestionToQuizTest, questionId = " + input.getQuestionId() + ", testId = " + input.getTestId());

        EduQuizTestQuizQuestion eduQuizTestQuizQuestion = eduQuizTestQuizQuestionService
            .createQuizTestQuestion(u, input);
        return ResponseEntity.ok().body(eduQuizTestQuizQuestion);
    }

    @PostMapping("/remove-question-from-quiz-test")
    public ResponseEntity<?> removeQuestionFromQuizTest(
        Principal principal,
        @RequestBody CreateQuizTestQuestionInputModel input
    ) {
        UserLogin u = userService.findById(principal.getName());
        log.info("removeQuestionFromQuizTest, questionId = " +
                 input.getQuestionId() +
                 ", testId = " +
                 input.getTestId());

        EduQuizTestQuizQuestion eduQuizTestQuizQuestion = eduQuizTestQuizQuestionService
            .removeQuizTestQuestion(u, input);
        return ResponseEntity.ok().body(eduQuizTestQuizQuestion);
    }

}
