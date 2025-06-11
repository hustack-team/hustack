package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamStudentEntity;
import com.hust.baseweb.applications.exam.model.response.ExamStudentResultDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamStudentUpdateDeleteRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamStudentRepository extends JpaRepository<ExamStudentEntity, String> {

    Optional<ExamStudentEntity> findByCode(String code);

    @Query(value = "select " +
                   "    distinct  " +
                   "    es.* " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_student_test est on " +
                   "    est.exam_student_id = es.id " +
                   "left join exam_exam_test eet on " +
                   "    eet.id = est.exam_exam_test_id " +
                   "where " +
                   "    eet.exam_id = :examId", nativeQuery = true)
    List<ExamStudentEntity> findALlByExamId(@Param("examId") String examId);

    @Query(value = "select " +
                   "    es.id, " +
                   "    COALESCE(json_agg(json_build_object('id', est.id, 'examStudentId', est.exam_student_id , 'examExamTestId', est.exam_exam_test_id)) FILTER (WHERE est.id IS NOT NULL), '[]') AS examStudentTests  " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_student_test est on " +
                   "    est.exam_student_id = es.id " +
                   "where " +
                   "    est.exam_exam_test_id in :examExamTestIds " +
                   "group by " +
                   "    es.id", nativeQuery = true)
    List<ExamStudentUpdateDeleteRes> findAllWithExamStudentTestByExamExamTestIds(@Param("examExamTestIds") List<String> examExamTestIds);

    @Query(value = "select " +
                   "    est.id as examStudentTestId, " +
                   "    es.id as id, " +
                   "    es.code as code, " +
                   "    es.name as name, " +
                   "    es.email as email, " +
                   "    es.phone as phone, " +
                   "    er.id as examResultId, " +
                   "    er.total_score as totalScore, " +
                   "    er.total_time as totalTime, " +
                   "    count(em.id) as totalViolate, " +
                   "    er.submit_again as submitAgain, " +
                   "    er.submited_at as submitedAt " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_student_test est on " +
                   "    es.id = est.exam_student_id " +
                   "left join exam_result er on " +
                   "    er.exam_student_test_id = est.id " +
                   "left join exam_monitor em on " +
                   "    er.id = em.exam_result_id " +
                   "where " +
                   "    est.exam_exam_test_id = :examExamTestId " +
                   "group by " +
                   "    est.id, " +
                   "    es.id, " +
                   "    es.code, " +
                   "    es.name, " +
                   "    es.email, " +
                   "    es.phone, " +
                   "    er.id, " +
                   "    er.total_score, " +
                   "    er.submited_at, " +
                   "    er.total_time " +
                   "order by " +
                   "    es.name " , nativeQuery = true)
    List<ExamStudentResultDetailsRes> findAllWithResult(@Param("examExamTestId") String examExamTestId);
}
