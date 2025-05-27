package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamTestEntity;
import com.hust.baseweb.applications.exam.model.response.ExamTestQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.MyExamTestWithResultRes;
import com.hust.baseweb.applications.exam.model.response.MyExamQuestionDetailsRes;
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
public interface ExamTestRepository extends JpaRepository<ExamTestEntity, String> {

    @Query(value = "select " +
                   "    * " +
                   "from " +
                   "    exam_test et " +
                   "where " +
                   "    et.created_by = :userLogin " +
                   "and " +
                   "    (:keyword is null or  " +
                   "    (lower(et.code) like CONCAT('%', lower(:keyword),'%')) or " +
                   "    (lower(et.name) like CONCAT('%', lower(:keyword),'%'))) " +
                   "and " +
                   "    (cast(cast(:createdFrom as text) as timestamp) is null or et.created_at >= cast(cast(:createdFrom as text) as timestamp)) " +
                   "and " +
                   "    (cast(cast(:createdTo as text) as timestamp) is null or et.created_at <= cast(cast(:createdTo as text) as timestamp)) " +
                   "order by et.created_at desc",
           countQuery = "select " +
                        "    count(1) " +
                        "from " +
                        "    exam_test et " +
                        "where " +
                        "    et.created_by = :userLogin " +
                        "and " +
                        "    (:keyword is null or  " +
                        "    (lower(et.code) like CONCAT('%', lower(:keyword),'%')) or " +
                        "    (lower(et.name) like CONCAT('%', lower(:keyword),'%'))) " +
                        "and " +
                        "    (cast(cast(:createdFrom as text) as timestamp) is null or et.created_at >= cast(cast(:createdFrom as text) as timestamp)) " +
                        "and " +
                        "    (cast(cast(:createdTo as text) as timestamp) is null or et.created_at <= cast(cast(:createdTo as text) as timestamp)) ",
           nativeQuery = true)
    Page<ExamTestEntity> filter(
        Pageable pageable,
        @Param("userLogin") String userLogin,
        @Param("createdFrom") LocalDateTime createdFrom,
        @Param("createdTo") LocalDateTime createdTo,
        @Param("keyword") String keyword
    );

    @Query(value = "select " +
                   "    etq.id as examTestQuestionId, " +
                   "    eq.id as questionId, " +
                   "    eq.code as questionCode, " +
                   "    eq.type as questionType, " +
                   "    eq.content as questionContent, " +
                   "    eq.file_path as questionFile, " +
                   "    eq.number_answer as questionNumberAnswer, " +
                   "    eq.multichoice as questionMultichoice, " +
                   "    eq.answer as questionAnswer, " +
                   "    eq.explain as questionExplain, " +
                   "    etq.order as questionOrder, " +
                   "    es.name as examSubjectName, " +
                   "    eq.level as questionLevel, " +
                   "    COALESCE( " +
                   "        (SELECT " +
                   "             json_agg(json_build_object('id', et.id, 'name', et.name)) " +
                   "         FROM exam_question_tag eqt " +
                   "         LEFT JOIN exam_tag et ON " +
                   "             et.id = eqt.exam_tag_id " +
                   "         WHERE eqt.exam_question_id = eq.id), '[]') AS questionExamTags, " +
                   "    COALESCE(json_agg(json_build_object('id', eqa.id, 'examQuestionId', eqa.exam_question_id, 'order', eqa.order, 'content', eqa.content, 'file', eqa.file) ORDER BY eqa.order ASC) FILTER (WHERE eqa.id IS NOT NULL), '[]') AS questionAnswers " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_test_question etq on " +
                   "    et.id = etq.exam_test_id " +
                   "left join exam_question eq on " +
                   "    etq.exam_question_id = eq.id " +
                   "left join exam_question_answer eqa on " +
                   "    eq.id = eqa.exam_question_id " +
                   "left join exam_subject es on " +
                   "    es.id = eq.exam_subject_id " +
                   "where " +
                   "    et.created_by = :userLogin " +
                   "    and et.id = :examTestId " +
                   "group by etq.id, eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, " +
                   "    eq.multichoice, eq.answer, eq.explain, etq.order, es.name, eq.level " +
                   "order by " +
                   "    etq.order", nativeQuery = true)
    List<ExamTestQuestionDetailsRes> details(@Param("userLogin") String userLogin,
                                             @Param("examTestId") String examTestId);

    @Query(value = "select " +
                   "    etq.id as examTestQuestionId, " +
                   "    eq.id as questionId, " +
                   "    eq.code as questionCode, " +
                   "    eq.type as questionType, " +
                   "    eq.content as questionContent, " +
                   "    eq.file_path as questionFile, " +
                   "    eq.number_answer as questionNumberAnswer, " +
                   "    eq.multichoice as questionMultichoice, " +
                   "    case when erd.score is not null and e.answer_status = 'OPEN' then eq.answer else null end as questionAnswer, " +
                   "    case when erd.score is not null and e.answer_status = 'OPEN' then eq.explain else null end as questionExplain, " +
                   "    etq.order as questionOrder, " +
                   "    erd.id as examResultDetailsId, " +
                   "    erd.answer as answer, " +
                   "    erd.file_path as filePathAnswer, " +
                   "    erd.comment_file_path as filePathComment, " +
                   "    erd.pass as pass, " +
                   "    erd.score as score, " +
                   "    COALESCE(json_agg(json_build_object('id', eqa.id, 'examQuestionId', eqa.exam_question_id, 'order', eqa.order, 'content', eqa.content, 'file', eqa.file) ORDER BY eqa.order ASC) FILTER (WHERE eqa.id IS NOT NULL), '[]') AS questionAnswers " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_test_question etq on " +
                   "    et.id = etq.exam_test_id " +
                   "left join exam_question eq on " +
                   "    etq.exam_question_id = eq.id " +
                   "left join exam_question_answer eqa on " +
                   "    eq.id = eqa.exam_question_id " +
                   "left join exam_exam_test eet on " +
                   "    eet.exam_test_id = et.id " +
                   "left join exam_student_test est on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "left join exam e on " +
                   "    e.id = eet.exam_id " +
                   "left join exam_result er on " +
                   "    er.exam_student_test_id = est.id  " +
                   "left join exam_result_details erd on " +
                   "    erd.exam_result_id = er.id " +
                   "    and erd.exam_question_id = eq.id " +
                   "where " +
                   "    est.id = :examStudentTestId " +
                   "group by " +
                   "    etq.id, eq.id, eq.code, eq.type, eq.content, eq.file_path, " +
                   "    eq.number_answer, eq.multichoice, e.answer_status, eq.answer,  " +
                   "    eq.explain, etq.order, erd.id, erd.answer, erd.file_path, " +
                   "    erd.comment_file_path, erd.pass, erd.score " +
                   "order by " +
                   "    etq.order", nativeQuery = true)
    List<MyExamQuestionDetailsRes> getMyExamQuestionDetails(@Param("examStudentTestId") String examStudentTestId);

    @Query(value = "select " +
                   "    etq.id as examTestQuestionId, " +
                   "    eq.id as questionId, " +
                   "    eq.code as questionCode, " +
                   "    eq.type as questionType, " +
                   "    eq.content as questionContent, " +
                   "    eq.file_path as questionFile, " +
                   "    eq.number_answer as questionNumberAnswer, " +
                   "    eq.multichoice as questionMultichoice, " +
                   "    eq.answer as questionAnswer, " +
                   "    eq.explain as questionExplain, " +
                   "    etq.order as questionOrder, " +
                   "    erd.id as examResultDetailsId, " +
                   "    erd.answer as answer, " +
                   "    erd.file_path as filePathAnswer, " +
                   "    erd.comment_file_path as filePathComment, " +
                   "    erd.pass as pass, " +
                   "    erd.score as score, " +
                   "    COALESCE(json_agg(json_build_object('id', eqa.id, 'examQuestionId', eqa.exam_question_id, 'order', eqa.order, 'content', eqa.content, 'file', eqa.file) ORDER BY eqa.order ASC) FILTER (WHERE eqa.id IS NOT NULL), '[]') AS questionAnswers " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_test_question etq on " +
                   "    et.id = etq.exam_test_id " +
                   "left join exam_question eq on " +
                   "    etq.exam_question_id = eq.id " +
                   "left join exam_question_answer eqa on " +
                   "    eq.id = eqa.exam_question_id " +
                   "left join exam_exam_test eet on " +
                   "    eet.exam_test_id = et.id " +
                   "left join exam_student_test est on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "left join exam_result er on " +
                   "    er.exam_student_test_id = est.id  " +
                   "left join exam_result_details erd on " +
                   "    erd.exam_result_id = er.id " +
                   "    and erd.exam_question_id = eq.id " +
                   "where " +
                   "    est.id = :examStudentTestId " +
                   "group by " +
                   "    etq.id, eq.id, eq.code, eq.type, eq.content, eq.file_path, " +
                   "    eq.number_answer, eq.multichoice, eq.answer, " +
                   "    eq.explain, etq.order, erd.id, erd.answer, erd.file_path, " +
                   "    erd.comment_file_path, erd.pass, erd.score " +
                   "order by " +
                   "    etq.order", nativeQuery = true)
    List<MyExamQuestionDetailsRes> getExamMarkingDetails(@Param("examStudentTestId") String examStudentTestId);

    Optional<ExamTestEntity> findByCode(String code);

    @Query(value = "select * from exam_test et where et.id in :examTestIds", nativeQuery = true)
    List<ExamTestEntity> findAllByExamTestIds(@Param("examTestIds") List<String> examTestIds);

    @Query(value = "select " +
                   "    est.id as examStudentTestId, " +
                   "    et.id as examTestId, " +
                   "    et.code as examTestCode, " +
                   "    et.name as examTestName, " +
                   "    et.duration as examTestDuration, " +
                   "    et.description as examTestDescription, " +
                   "    er.id as examResultId, " +
                   "    er.total_score as totalScore, " +
                   "    er.total_time as totalTime, " +
                   "    count(em.id) as totalViolate " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_exam_test eet on " +
                   "    et.id = eet.exam_test_id " +
                   "left join exam e on " +
                   "    e.id = eet.exam_id " +
                   "left join exam_student_test est on " +
                   "    est.exam_exam_test_id = eet.id " +
                   "left join exam_student es on " +
                   "    es.id = est.exam_student_id " +
                   "left join exam_result er on " +
                   "    er.exam_student_test_id = est.id " +
                   "left join exam_monitor em on " +
                   "    er.id = em.exam_result_id " +
                   "where " +
                   "    es.code = :userLogin " +
                   "    and e.id = :examId " +
                   "group by " +
                   "    est.id, " +
                   "    et.id, " +
                   "    et.code, " +
                   "    et.name, " +
                   "    et.duration, " +
                   "    et.description, " +
                   "    er.id, " +
                   "    er.total_score, " +
                   "    er.total_time ", nativeQuery = true)
    List<MyExamTestWithResultRes> findAllWithResultByExamId(@Param("userLogin") String userLogin,
                                                                 @Param("examId") String examId);
}
