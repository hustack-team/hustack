package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamQuestionTagEntity;
import com.hust.baseweb.applications.exam.entity.ExamQuestionTagKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ExamQuestionTagRepository extends JpaRepository<ExamQuestionTagEntity, ExamQuestionTagKey> {

    List<ExamQuestionTagEntity> findALLById_ExamQuestionId(String examQuestionId);
}
