package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamQuestionAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionAnswerRepository extends JpaRepository<ExamQuestionAnswerEntity, String> {

    List<ExamQuestionAnswerEntity> findAllByExamQuestionId(String examQuestionId);
}
