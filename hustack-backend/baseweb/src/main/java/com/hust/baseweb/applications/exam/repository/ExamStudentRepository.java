package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamStudentEntity;
import com.hust.baseweb.applications.exam.model.response.ExamStudentResultDetailsRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamStudentRepository extends JpaRepository<ExamStudentEntity, String> {

    Optional<ExamStudentEntity> findByCode(String code);
    Optional<ExamStudentEntity> findByCodeAndExamId(String code, String examId);
    List<ExamStudentEntity> findALlByExamId(String examId);

    @Query(value = "select " +
                   "    es.id as id, " +
                   "    es.code as code, " +
                   "    es.name as name, " +
                   "    es.email as email, " +
                   "    es.phone as phone, " +
                   "    er.id as examResultId, " +
                   "    er.total_score as totalScore, " +
                   "    er.total_time as totalTime, " +
                   "    er.submited_at as submitedAt " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_result er on " +
                   "    es.id = er.exam_student_id " +
                   "where " +
                   "    es.exam_id =:examId " +
                   "order by " +
                   "    es.name", nativeQuery = true)
    List<ExamStudentResultDetailsRes> findAllWithResult(@Param("examId") String examId);
}
