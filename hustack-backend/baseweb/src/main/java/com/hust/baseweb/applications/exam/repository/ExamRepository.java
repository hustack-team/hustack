package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamEntity;
import com.hust.baseweb.applications.exam.model.response.ExamDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamMarkingDetailsResDB;
import com.hust.baseweb.applications.exam.model.response.MyExamDetailsResDB;
import com.hust.baseweb.applications.exam.model.response.MyExamFilterRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<ExamEntity, String> {

    @Query(value = "select " +
                   "    * " +
                   "from " +
                   "    exam e " +
                   "where " +
                   "    e.created_by = :userLogin " +
                   "and " +
                   "    (:status is null or e.status = :status) " +
                   "and " +
                   "    (:keyword is null or " +
                   "    (lower(e.code) like CONCAT('%', lower(:keyword),'%')) or " +
                   "    (lower(e.name) like CONCAT('%', lower(:keyword),'%'))) " +
                   "and " +
                   "    (cast(cast(:startTimeFrom as text) as timestamp) is null or e.start_time >= cast(cast(:startTimeFrom as text) as timestamp)) " +
                   "and " +
                   "    (cast(cast(:startTimeTo as text) as timestamp) is null or e.start_time <= cast(cast(:startTimeTo as text) as timestamp)) " +
                   "and " +
                   "    (cast(cast(:endTimeFrom as text) as timestamp) is null or e.end_time >= cast(cast(:endTimeFrom as text) as timestamp)) " +
                   "and " +
                   "    (cast(cast(:endTimeTo as text) as timestamp) is null or e.end_time <= cast(cast(:endTimeTo as text) as timestamp)) " +
                   "order by e.start_time desc",
           countQuery = "select " +
                        "    count(1) " +
                        "from " +
                        "    exam e " +
                        "where " +
                        "    e.created_by = :userLogin " +
                        "and " +
                        "    (:status is null or e.status = :status) " +
                        "and " +
                        "    (:keyword is null or " +
                        "    (lower(e.code) like CONCAT('%', lower(:keyword),'%')) or " +
                        "    (lower(e.name) like CONCAT('%', lower(:keyword),'%'))) " +
                        "and " +
                        "    (cast(cast(:startTimeFrom as text) as timestamp) is null or e.start_time >= cast(cast(:startTimeFrom as text) as timestamp)) " +
                        "and " +
                        "    (cast(cast(:startTimeTo as text) as timestamp) is null or e.start_time <= cast(cast(:startTimeTo as text) as timestamp)) " +
                        "and " +
                        "    (cast(cast(:endTimeFrom as text) as timestamp) is null or e.end_time >= cast(cast(:endTimeFrom as text) as timestamp)) " +
                        "and " +
                        "    (cast(cast(:endTimeTo as text) as timestamp) is null or e.end_time <= cast(cast(:endTimeTo as text) as timestamp)) ",
           nativeQuery = true)
    Page<ExamEntity> filter(
        Pageable pageable,
        @Param("userLogin") String userLogin,
        @Param("status") Integer status,
        @Param("startTimeFrom") LocalDateTime startTimeFrom,
        @Param("startTimeTo") LocalDateTime startTimeTo,
        @Param("endTimeFrom") LocalDateTime endTimeFrom,
        @Param("endTimeTo") LocalDateTime endTimeTo,
        @Param("keyword") String keyword
    );

    @Query(value = "select  " +
                   "    e.id, " +
                   "    e.code, " +
                   "    e.name, " +
                   "    e.description, " +
                   "    e.status, " +
                   "    e.answer_status as answerStatus, " +
                   "    e.start_time as startTime, " +
                   "    e.end_time as endTime, " +
                   "    COALESCE(json_agg(json_build_object('id', et.id, 'examExamTestId', eet.id, 'code', et.code, 'name', et.name, 'description', et.description)) FILTER (WHERE et.id IS NOT NULL), '[]') AS examTests  " +
                   "from  " +
                   "    exam e " +
                   "left join exam_exam_test eet on " +
                   "    eet.exam_id = e.id " +
                   "left join exam_test et on " +
                   "    eet.exam_test_id = et.id " +
                   "where " +
                   "    e.id = :id " +
                   "group by  " +
                   "    e.id, e.code, e.name, e.description, e.status,  " +
                   "    e.answer_status, e.start_time, e.end_time", nativeQuery = true)
    Optional<ExamDetailsRes> detailExamById(@Param("id") String id);

    Optional<ExamEntity> findByCode(String code);

    @Query(value = "select " +
                   "    e.* " +
                   "from " +
                   "    exam e " +
                   "left join exam_exam_test eet on " +
                   "    eet.exam_id = e.id " +
                   "where " +
                   "    eet.exam_test_id = :examTestId", nativeQuery = true)
    List<ExamEntity> findALlByExamTestId(String examTestId);

    @Query(value = "select  " +
                   "    e.id as examId, " +
                   "    e.code as examCode, " +
                   "    e.name as examName, " +
                   "    e.description as examDescription, " +
                   "    e.start_time as startTime, " +
                   "    e.end_time as endTime " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_student_test est on " +
                   "    est.exam_student_id = es.id " +
                   "left join exam_exam_test eet on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "left join exam e on " +
                   "    eet.exam_id = e.id " +
                   "where " +
                   "    es.code = :userLogin " +
                   "    and e.status = 1 " +
                   "    and  " +
                   "        (:keyword is null or  " +
                   "        (lower(e.description) like CONCAT('%', lower(:keyword),'%')) or  " +
                   "        (lower(e.name) like CONCAT('%', lower(:keyword),'%')))  " +
                   "group by " +
                   "    e.id, " +
                   "    e.code, " +
                   "    e.name, " +
                   "    e.description, " +
                   "    e.start_time, " +
                   "    e.end_time " +
                   "order by " +
                   "    e.start_time desc",
           countQuery = "select " +
                        "    count(1) " +
                        "from " +
                        "    exam_student es " +
                        "left join exam_student_test est on " +
                        "    est.exam_student_id = es.id " +
                        "left join exam_exam_test eet on " +
                        "    est.exam_exam_test_id = eet.id " +
                        "left join exam e on " +
                        "    eet.exam_id = e.id " +
                        "where " +
                        "    es.code = :userLogin " +
                        "    and e.status = 1 " +
                        "    and  " +
                        "        (:keyword is null or  " +
                        "        (lower(e.description) like CONCAT('%', lower(:keyword),'%')) or  " +
                        "        (lower(e.name) like CONCAT('%', lower(:keyword),'%')))  " +
                        "group by " +
                        "    e.id, " +
                        "    e.code, " +
                        "    e.name, " +
                        "    e.description, " +
                        "    e.start_time, " +
                        "    e.end_time ",
           nativeQuery = true)
    Page<MyExamFilterRes> filterMyExam(
        Pageable pageable,
        @Param("userLogin") String userLogin,
        @Param("status") Integer status,
        @Param("keyword") String keyword
    );

    @Query(value = "select " +
                   "    est.id as examStudentTestId, " +
                   "    e.id as examId, " +
                   "    e.code as examCode, " +
                   "    e.name as examName, " +
                   "    e.description as examDescription, " +
                   "    e.start_time as startTime, " +
                   "    e.end_time as endTime, " +
                   "    et.id as examTestId, " +
                   "    et.code as examTestCode, " +
                   "    et.name as examTestName, " +
                   "    er.id as examResultId, " +
                   "    er.total_score as totalScore, " +
                   "    er.total_time as totalTime, " +
                   "    er.submited_at as submitedAt, " +
                   "    er.file_path as answerFiles, " +
                   "    er.comment as comment, " +
                   "    e.answer_status as examAnswerStatus " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_student_test est on " +
                   "    est.exam_student_id = es.id " +
                   "left join exam_exam_test eet on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "left join exam e on " +
                   "    e.id = eet.exam_id " +
                   "left join exam_test et on " +
                   "    et.id = eet.exam_test_id " +
                   "left join exam_result er on " +
                   "    er.exam_student_test_id = est.id " +
                   "where " +
                   "    es.code = :userLogin " +
                   "    and est.id = :examStudentTestId " +
                   "    and e.status = 1 " +
                   "order by start_time desc",
           nativeQuery = true)
    Optional<MyExamDetailsResDB> detailsMyExam(
        @Param("userLogin") String userLogin,
        @Param("examStudentTestId") String examStudentTestId
    );

    @Query(value = "select " +
                   "    eet.exam_id as examId, " +
                   "    est.id as examStudentTestId, " +
                   "    es.id as examStudentId, " +
                   "    es.code as examStudentCode, " +
                   "    es.name as examStudentName, " +
                   "    es.email as examStudentEmail, " +
                   "    es.phone as examStudentPhone, " +
                   "    er.id as examResultId, " +
                   "    er.total_score as totalScore, " +
                   "    er.total_time as totalTime, " +
                   "    er.submited_at as submitedAt, " +
                   "    er.file_path as answerFiles, " +
                   "    er.comment as comment " +
                   "from " +
                   "    exam_student es " +
                   "left join exam_student_test est on " +
                   "    est.exam_student_id = es.id " +
                   "left join exam_exam_test eet on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "left join exam_result er on " +
                   "    er.exam_student_test_id = est.id " +
                   "where " +
                   "    est.id = :examStudentTestId ",
           nativeQuery = true)
    Optional<ExamMarkingDetailsResDB> detailsExamMarking(
        @Param("examStudentTestId") String examStudentTestId
    );
}
