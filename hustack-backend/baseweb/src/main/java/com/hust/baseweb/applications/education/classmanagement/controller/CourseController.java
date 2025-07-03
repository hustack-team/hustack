package com.hust.baseweb.applications.education.classmanagement.controller;

import com.hust.baseweb.applications.education.entity.EduCourse;
import com.hust.baseweb.applications.education.entity.EduCourseSession;
import com.hust.baseweb.applications.education.entity.EduCourseSessionInteractiveQuiz;
import com.hust.baseweb.applications.education.model.AddCourseModel;
import com.hust.baseweb.applications.education.model.CourseSessionInteractiveQuizCreateModel;
import com.hust.baseweb.applications.education.model.EduCourseSessionModelCreate;
import com.hust.baseweb.applications.education.repo.EduCourseSessionInteractiveQuizRepo;
import com.hust.baseweb.applications.education.repo.EduCourseSessionRepo;
import com.hust.baseweb.applications.education.service.CourseService;
import com.hust.baseweb.applications.education.service.EduCourseSessionInteractiveQuizService;
import com.hust.baseweb.applications.education.service.EduCourseSessionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@Validated
@RequestMapping("/edu/course")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {

    CourseService courseService;

    EduCourseSessionRepo eduCourseSessionRepo;

    EduCourseSessionService eduCourseSessionService;

    EduCourseSessionInteractiveQuizService eduCourseSessionInteractiveQuizService;

    EduCourseSessionInteractiveQuizRepo eduCourseSessionInteractiveQuizRepo;

    @PostMapping("/create")
    public ResponseEntity<?> addEduClass(Principal principal, @RequestBody AddCourseModel addCourseModel) {
        log.info("addEduClass, start....");
        EduCourse eduCourse = courseService.createCourse(addCourseModel.getCourseId(), addCourseModel.getCourseName(), addCourseModel.getCredit());
        if (eduCourse != null) {
            return ResponseEntity.ok().body(eduCourse);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/create-course-session")
    public ResponseEntity<?> createSessionOfCourse(
        Principal principal,
        @RequestBody EduCourseSessionModelCreate eduCourseSessionModelCreate
    ) {
        EduCourseSession eduCourseSession = eduCourseSessionService.createCourseSession(eduCourseSessionModelCreate.getCourseId(), eduCourseSessionModelCreate.getSessionName(), principal.getName(), eduCourseSessionModelCreate.getDescription());
        return ResponseEntity.ok().body(eduCourseSession);
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/get-course-sessions/{courseId}")
    public ResponseEntity<?> getSessionOfCourse(Principal principal, @PathVariable String courseId){
        List<EduCourseSession> eduCourseSessions = eduCourseSessionRepo.findByCourseId(courseId);
        return ResponseEntity.ok().body(eduCourseSessions);
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/create-course-session-interactive-quiz")
    public ResponseEntity<?> createCourseSessionInteractiveQuiz(
        Principal principal,
        @RequestBody CourseSessionInteractiveQuizCreateModel courseSessionInteractiveQuizCreateModel
    ) {
        return ResponseEntity.ok().body(eduCourseSessionInteractiveQuizService.createCourseSessionInteractiveQuiz(courseSessionInteractiveQuizCreateModel.getInteractiveQuizName(), courseSessionInteractiveQuizCreateModel.getSessionId(), courseSessionInteractiveQuizCreateModel.getDescription()));
    }
    
    @Secured("ROLE_TEACHER")
    @GetMapping("/get-interactive-quiz-of-course-session/{sessionId}")
    public ResponseEntity<?> getInteractiveQuizOfCourseSession(Principal principal, @PathVariable UUID sessionId){
        List<EduCourseSessionInteractiveQuiz> eduCourseSessionInteractiveQuizs = eduCourseSessionInteractiveQuizRepo.findBySessionId(sessionId);
        return ResponseEntity.ok().body(eduCourseSessionInteractiveQuizs);
    }

}
