package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamTestEntity;
import com.hust.baseweb.applications.exam.model.response.ExamTestQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.MyExamQuestionDetailsRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTestRepository extends JpaRepository<ExamTestEntity, String> {

    @Query(value = "select " +
                   "    etq.id as examTestQuestionId, " +
                   "    eq.id as questionId, " +
                   "    eq.code as questionCode, " +
                   "    eq.type as questionType, " +
                   "    eq.content as questionContent, " +
                   "    eq.file_path as questionFile, " +
                   "    eq.number_answer as questionNumberAnswer, " +
                   "    eq.content_answer1 as questionContentAnswer1, " +
                   "    eq.content_answer2 as questionContentAnswer2, " +
                   "    eq.content_answer3 as questionContentAnswer3, " +
                   "    eq.content_answer4 as questionContentAnswer4, " +
                   "    eq.content_answer5 as questionContentAnswer5, " +
                   "    eq.multichoice as questionMultichoice, " +
                   "    eq.answer as questionAnswer, " +
                   "    eq.explain as questionExplain, " +
                   "    etq.order as questionOrder, " +
                   "    es.name as examSubjectName, " +
                   "    eq.level as questionLevel, " +
                   "    string_agg(eta.id, ',') as examTagIdStr, " +
                   "    string_agg(eta.name, ',') as examTagNameStr " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_test_question etq on " +
                   "    et.id = etq.exam_test_id " +
                   "left join exam_question eq on " +
                   "    etq.exam_question_id = eq.id " +
                   "left join exam_subject es on " +
                   "    es.id = eq.exam_subject_id " +
                   "left join exam_question_tag eqt on " +
                   "    eqt.exam_question_id = eq.id " +
                   "left join exam_tag eta on " +
                   "    eta.id = eqt.exam_tag_id " +
                   "where " +
                   "    et.created_by = :userLogin " +
                   "    and et.id = :examTestId " +
                   "group by etq.id, eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, " +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3, eq.content_answer4, " +
                   "    eq.content_answer5, eq.multichoice, eq.answer, eq.explain, etq.order, es.name, eq.level " +
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
                   "    eq.content_answer1 as questionContentAnswer1, " +
                   "    eq.content_answer2 as questionContentAnswer2, " +
                   "    eq.content_answer3 as questionContentAnswer3, " +
                   "    eq.content_answer4 as questionContentAnswer4, " +
                   "    eq.content_answer5 as questionContentAnswer5, " +
                   "    eq.multichoice as questionMultichoice, " +
                   "    case when erd.score is not null and e.answer_status = 'OPEN' then eq.answer else null end as questionAnswer, " +
                   "    case when erd.score is not null and e.answer_status = 'OPEN' then eq.explain else null end as questionExplain, " +
                   "    etq.order as questionOrder, " +
                   "    erd.answer as answer, " +
                   "    erd.file_path as filePathAnswer, " +
                   "    erd.pass as pass, " +
                   "    erd.score as score " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_test_question etq on " +
                   "    et.id = etq.exam_test_id " +
                   "left join exam_question eq on " +
                   "    etq.exam_question_id = eq.id " +
                   "left join exam_student es on " +
                   "    es.exam_test_id = et.id " +
                   "left join exam e on " +
                   "    e.id = es.exam_id " +
                   "left join exam_result er on " +
                   "    es.id = er.exam_student_id " +
                   "left join exam_result_details erd on " +
                   "    erd.exam_result_id = er.id " +
                   "    and erd.exam_question_id = eq.id " +
                   "where " +
                   "    et.id = :examTestId " +
                   "    and es.id = :examStudentId " +
                   "order by " +
                   "    etq.order", nativeQuery = true)
    List<MyExamQuestionDetailsRes> getMyExamQuestionDetails(@Param("examTestId") String examTestId,
                                                            @Param("examStudentId") String examStudentId);

    @Query(value = "select " +
                   "    etq.id as examTestQuestionId, " +
                   "    eq.id as questionId, " +
                   "    eq.code as questionCode, " +
                   "    eq.type as questionType, " +
                   "    eq.content as questionContent, " +
                   "    eq.file_path as questionFile, " +
                   "    eq.number_answer as questionNumberAnswer, " +
                   "    eq.content_answer1 as questionContentAnswer1, " +
                   "    eq.content_answer2 as questionContentAnswer2, " +
                   "    eq.content_answer3 as questionContentAnswer3, " +
                   "    eq.content_answer4 as questionContentAnswer4, " +
                   "    eq.content_answer5 as questionContentAnswer5, " +
                   "    eq.multichoice as questionMultichoice, " +
                   "    eq.answer as questionAnswer, " +
                   "    eq.explain as questionExplain, " +
                   "    etq.order as questionOrder, " +
                   "    erd.id as examResultDetailsId, " +
                   "    erd.answer as answer, " +
                   "    erd.file_path as filePathAnswer, " +
                   "    erd.pass as pass, " +
                   "    erd.score as score " +
                   "from " +
                   "    exam_test et " +
                   "left join exam_test_question etq on " +
                   "    et.id = etq.exam_test_id " +
                   "left join exam_question eq on " +
                   "    etq.exam_question_id = eq.id " +
                   "left join exam_student es on " +
                   "    es.exam_test_id = et.id " +
                   "left join exam_result er on " +
                   "    es.id = er.exam_student_id " +
                   "left join exam_result_details erd on " +
                   "    erd.exam_result_id = er.id " +
                   "    and erd.exam_question_id = eq.id " +
                   "where " +
                   "    et.id = :examTestId " +
                   "    and es.id = :examStudentId " +
                   "order by " +
                   "    etq.order", nativeQuery = true)
    List<MyExamQuestionDetailsRes> getExamMarkingDetails(@Param("examTestId") String examTestId,
                                                            @Param("examStudentId") String examStudentId);

    Optional<ExamTestEntity> findByCode(String code);
}
