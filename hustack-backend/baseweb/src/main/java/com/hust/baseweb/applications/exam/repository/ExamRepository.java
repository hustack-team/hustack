package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamEntity;
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

    Optional<ExamEntity> findByCode(String code);

    List<ExamEntity> findALlByExamTestId(String examTestId);

    @Query(value = "select " +
                   "    es.id as examStudentId, " +
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
                   "    er.total_score as totalScore " +
                   "from " +
                   "    exam_student es " +
                   "left join exam e on " +
                   "    e.id = es.exam_id " +
                   "left join exam_test et on " +
                   "    et.id = es.exam_test_id " +
                   "left join exam_result er on " +
                   "    er.exam_student_id = es.id " +
                   "where " +
                   "    es.code = :userLogin " +
                   "    and e.status = 1  " +
                   "and " +
                   "    (:status is null " +
                   "        or (:status = 0 and er.id is null and er.total_score is null) " +
                   "        or (:status = 1 and er.id is not null and er.total_score is null) " +
                   "        or (:status = 2 and er.id is not null and er.total_score is not null)) " +
                   "and " +
                   "    (:keyword is null or " +
                   "    (lower(et.name) like CONCAT('%', lower(:keyword),'%')) or " +
                   "    (lower(e.name) like CONCAT('%', lower(:keyword),'%'))) " +
                   "order by e.start_time desc",
           countQuery = "select " +
                        "    count(1) " +
                        "from " +
                        "    exam_student es " +
                        "left join exam e on " +
                        "    e.id = es.exam_id " +
                        "left join exam_test et on " +
                        "    et.id = es.exam_test_id " +
                        "left join exam_result er on " +
                        "    er.exam_student_id = es.id " +
                        "where " +
                        "    es.code = :userLogin " +
                        "    and e.status = 1  " +
                        "and " +
                        "    (:status is null " +
                        "        or (:status = 0 and er.id is null and er.total_score is null) " +
                        "        or (:status = 1 and er.id is not null and er.total_score is null) " +
                        "        or (:status = 2 and er.id is not null and er.total_score is not null)) " +
                        "and " +
                        "    (:keyword is null or " +
                        "    (lower(et.name) like CONCAT('%', lower(:keyword),'%')) or " +
                        "    (lower(e.name) like CONCAT('%', lower(:keyword),'%'))) ",
           nativeQuery = true)
    Page<MyExamFilterRes> filterMyExam(
        Pageable pageable,
        @Param("userLogin") String userLogin,
        @Param("status") Integer status,
        @Param("keyword") String keyword
    );

    @Query(value = "select " +
                   "    es.id as examStudentId, " +
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
                   "left join exam e on " +
                   "    e.id = es.exam_id " +
                   "left join exam_test et on " +
                   "    et.id = es.exam_test_id " +
                   "left join exam_result er on " +
                   "    er.exam_student_id = es.id " +
                   "where " +
                   "    es.code = :userLogin " +
                   "    and e.id = :examId " +
                   "    and es.id = :examStudentId " +
                   "    and e.status = 1 " +
                   "order by start_time desc",
           nativeQuery = true)
    Optional<MyExamDetailsResDB> detailsMyExam(
        @Param("userLogin") String userLogin,
        @Param("examId") String examId,
        @Param("examStudentId") String examStudentId
    );

    @Query(value = "select " +
                   "    es.exam_id as examId, " +
                   "    es.exam_test_id as examTestId, " +
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
                   "left join exam_result er on " +
                   "    er.exam_student_id = es.id " +
                   "where " +
                   "    es.id = :examStudentId ",
           nativeQuery = true)
    Optional<ExamMarkingDetailsResDB> detailsExamMarking(
        @Param("examStudentId") String examStudentId
    );
}
