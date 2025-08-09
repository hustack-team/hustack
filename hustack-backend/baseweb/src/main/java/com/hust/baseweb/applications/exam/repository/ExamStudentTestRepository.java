package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamStudentTestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamStudentTestRepository extends JpaRepository<ExamStudentTestEntity, String> {

    List<ExamStudentTestEntity> findAllByExamExamTestId(String examExamTestId);
    List<ExamStudentTestEntity> findAllByExamStudentId(String examStudentId);
}
