package com.hust.baseweb.applications.education.entity;

import com.hust.baseweb.applications.education.entity.compositeid.CompositeCourseSessionInteractiveQuizQuestionId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "edu_course_session_interactive_quiz_question")
@IdClass(CompositeCourseSessionInteractiveQuizQuestionId.class)
public class EduCourseSessionInteractiveQuizQuestion {
    @Id
    @Column(name = "interactive_quiz_id")
    private UUID interactiveQuizId;

    @Id
    @Column(name = "quiz_question_id")
    private UUID questionId;

    @Column(name = "created_stamp")
    private Date createdStamp;

    @Column(name = "last_updated")
    private Date lastUpdated;
}
