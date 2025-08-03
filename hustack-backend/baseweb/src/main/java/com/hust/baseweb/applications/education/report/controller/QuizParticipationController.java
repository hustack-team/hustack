package com.hust.baseweb.applications.education.report.controller;

import com.hust.baseweb.applications.education.report.model.quizparticipation.GetQuizParticipationStatisticInputModel;
import com.hust.baseweb.applications.education.report.model.quizparticipation.QuizParticipationStatisticOutputModel;
import com.hust.baseweb.applications.education.report.service.quizparticipation.QuizParticipationStatisticService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizParticipationController {

    QuizParticipationStatisticService quizParticipationStatisticService;

    @PostMapping("/get-quiz-participation-statistic")
    public ResponseEntity<?> getQuizParticipationStatistic(
        Principal principal,
        @RequestBody GetQuizParticipationStatisticInputModel input
    ) {
        List<QuizParticipationStatisticOutputModel> quizParticipationStatisticOutputModelList =
            quizParticipationStatisticService.getQuizParticipationStatistic(input);

        return ResponseEntity.ok().body(quizParticipationStatisticOutputModelList);
    }
}
