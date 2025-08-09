package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamExamTestEntity;
import com.hust.baseweb.applications.exam.model.response.ExamExamTestPreviewUpdateRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamExamTestRepository extends JpaRepository<ExamExamTestEntity, String> {

    List<ExamExamTestEntity> findAllByExamId(String examId);

    @Query(value = "select  " +
                   "    eet.id, " +
                   "    eet.exam_id as examId, " +
                   "    eet.exam_test_id as examTestId, " +
                   "    et.code as examTestCode, " +
                   "    et.name as examTestName, " +
                   "    et.description as examTestDescription " +
                   "from " +
                   "    exam_exam_test eet " +
                   "left join exam_test et on " +
                   "    et.id = eet.exam_test_id " +
                   "where " +
                   "    eet.exam_id = :examId", nativeQuery = true)
    List<ExamExamTestPreviewUpdateRes> findPreviewUpdateByExamId(String examId);
}
