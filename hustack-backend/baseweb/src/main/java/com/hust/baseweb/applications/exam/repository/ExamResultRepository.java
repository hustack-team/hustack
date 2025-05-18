package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResultEntity, String> {

    @Query(value = "select " +
                   "    er.* " +
                   "from " +
                   "    exam_result er " +
                   "left join exam_student_test est on " +
                   "    er.exam_student_test_id = est.id " +
                   "left join exam_exam_test eet on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "where " +
                   "    eet.exam_id = :examId", nativeQuery = true)
    List<ExamResultEntity> findAllByExamId(String examId);
}
