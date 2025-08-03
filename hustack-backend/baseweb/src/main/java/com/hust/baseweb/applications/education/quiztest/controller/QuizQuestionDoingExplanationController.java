package com.hust.baseweb.applications.education.quiztest.controller;

import com.hust.baseweb.applications.education.quiztest.model.quizdoingexplanation.QuizDoingExplanationInputModel;
import com.hust.baseweb.applications.education.quiztest.service.QuizQuestionDoingExplanationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@RequestMapping("/quiz-doing-explanations")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizQuestionDoingExplanationController {

    QuizQuestionDoingExplanationService quizDoingExplanationService;

    @GetMapping("/{questionId}")
    public ResponseEntity<?> getParticipantExplanationForQuestion(
        Principal principal,
        @PathVariable UUID questionId
    ) {
        return ResponseEntity.ok(
            quizDoingExplanationService.findExplanationByParticipantIdAndQuestionId(principal.getName(), questionId)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createQuizDoingExplanation(
        Principal principal,
        @RequestPart QuizDoingExplanationInputModel quizDoingExplanation,
        @RequestPart(required = false) MultipartFile attachment
    ) {
        log.info("Create quiz doing explanation {}", quizDoingExplanation);
        try {
            quizDoingExplanation.setParticipantUserId(principal.getName());
            return ResponseEntity.ok(quizDoingExplanationService.createExplanation(quizDoingExplanation, attachment));
        } catch (RuntimeException e) {
            log.error("An error occur when create quiz doing explanation", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{explanationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateQuizDoingExplanation(
        @PathVariable UUID explanationId,
        @RequestPart(required = false) String solutionExplanation,
        @RequestPart(required = false) MultipartFile attachment
    ) {
        log.info("Update quiz doing explanation with new explanation = {}", solutionExplanation);
        try {
            return ResponseEntity.ok(
                quizDoingExplanationService.updateExplanation(explanationId, solutionExplanation, attachment)
            );
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("An error occur when update quiz doing explanation with id = {}. Detail: {}",
                      explanationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{explanationId}")
    public ResponseEntity deleteQuizDoingExplanation(@PathVariable UUID explanationId) {
        log.info("Delete quiz doing explanation having id = {}", explanationId);
        try {
            quizDoingExplanationService.deleteExplanation(explanationId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("An error occur when delete quiz doing explanation with id = {}. Detail: {}",
                      explanationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
