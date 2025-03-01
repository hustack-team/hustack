package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamQuestionEntity;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestionEntity, String> {
    Optional<ExamQuestionEntity> findByCode(String code);

    @Query(value = "select " +
                   "    eq.id, " +
                   "    eq.code, " +
                   "    eq.type, " +
                   "    eq.content, " +
                   "    eq.file_path as filePath, " +
                   "    eq.number_answer as numberAnswer, " +
                   "    eq.content_answer1 as contentAnswer1, " +
                   "    eq.content_answer2 as contentAnswer2, " +
                   "    eq.content_answer3 as contentAnswer3, " +
                   "    eq.content_answer4 as contentAnswer4, " +
                   "    eq.content_answer5 as contentAnswer5, " +
                   "    eq.multichoice, " +
                   "    eq.answer, " +
                   "    eq.explain, " +
                   "    eq.created_at as createdAt, " +
                   "    eq.updated_at as updatedAt, " +
                   "    eq.created_by as createdBy, " +
                   "    eq.updated_by as updatedBy, " +
                   "    es.name as examSubjectName, " +
                   "    eq.level, " +
                   "    string_agg(et.id, ',') as examTagIdStr, " +
                   "    string_agg(et.name, ',') as examTagNameStr " +
                   "from " +
                   "    exam_question eq " +
                   "left join exam_subject es on " +
                   "    es.id = eq.exam_subject_id " +
                   "left join exam_question_tag eqt on " +
                   "    eqt.exam_question_id = eq.id " +
                   "left join exam_tag et on " +
                   "    et.id = eqt.exam_tag_id " +
                   "where " +
                   "    eq.id = :id " +
                   "group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer,  " +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3,  " +
                   "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer,  " +
                   "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by,  " +
                   "    es.name, eq.level", nativeQuery = true)
    Optional<ExamQuestionDetailsRes> findOneById(String id);

    @Query(value = "select " +
                   "    eq.id, " +
                   "    eq.code, " +
                   "    eq.type, " +
                   "    eq.content, " +
                   "    eq.file_path as filePath, " +
                   "    eq.number_answer as numberAnswer, " +
                   "    eq.content_answer1 as contentAnswer1, " +
                   "    eq.content_answer2 as contentAnswer2, " +
                   "    eq.content_answer3 as contentAnswer3, " +
                   "    eq.content_answer4 as contentAnswer4, " +
                   "    eq.content_answer5 as contentAnswer5, " +
                   "    eq.multichoice, " +
                   "    eq.answer, " +
                   "    eq.explain, " +
                   "    eq.created_at as createdAt, " +
                   "    eq.updated_at as updatedAt, " +
                   "    eq.created_by as createdBy, " +
                   "    eq.updated_by as updatedBy, " +
                   "    es.name as examSubjectName, " +
                   "    eq.level, " +
                   "    string_agg(et.id, ',') as examTagIdStr, " +
                   "    string_agg(et.name, ',') as examTagNameStr " +
                   "from " +
                   "    exam_question eq " +
                   "left join exam_subject es on " +
                   "    es.id = eq.exam_subject_id " +
                   "left join exam_question_tag eqt on " +
                   "    eqt.exam_question_id = eq.id " +
                   "left join exam_tag et on " +
                   "    et.id = eqt.exam_tag_id " +
                   "where " +
                   "    eq.code = :code " +
                   "group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer,  " +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3,  " +
                   "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer,  " +
                   "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by,  " +
                   "    es.name, eq.level", nativeQuery = true)
    Optional<ExamQuestionDetailsRes> findOneByCode(String code);

    List<ExamQuestionEntity> findAllByExamSubjectId(String examSubjectId);
}
