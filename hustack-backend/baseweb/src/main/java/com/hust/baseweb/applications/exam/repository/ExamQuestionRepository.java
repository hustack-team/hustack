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

    @Query(value = "select\n" +
                   "    eq.id,\n" +
                   "    eq.code,\n" +
                   "    eq.type,\n" +
                   "    eq.content,\n" +
                   "    eq.file_path as filePath,\n" +
                   "    eq.number_answer as numberAnswer,\n" +
                   "    eq.content_answer1 as contentAnswer1,\n" +
                   "    eq.content_answer2 as contentAnswer2,\n" +
                   "    eq.content_answer3 as contentAnswer3,\n" +
                   "    eq.content_answer4 as contentAnswer4,\n" +
                   "    eq.content_answer5 as contentAnswer5,\n" +
                   "    eq.multichoice,\n" +
                   "    eq.answer,\n" +
                   "    eq.explain,\n" +
                   "    eq.created_at as createdAt,\n" +
                   "    eq.updated_at as updatedAt,\n" +
                   "    eq.created_by as createdBy,\n" +
                   "    eq.updated_by as updatedBy,\n" +
                   "    es.name as examSubjectName,\n" +
                   "    eq.level,\n" +
                   "    string_agg(et.id, ',') as examTagIdStr,\n" +
                   "    string_agg(et.name, ',') as examTagNameStr\n" +
                   "from\n" +
                   "    exam_question eq\n" +
                   "left join exam_subject es on\n" +
                   "    es.id = eq.exam_subject_id\n" +
                   "left join exam_question_tag eqt on\n" +
                   "    eqt.exam_question_id = eq.id\n" +
                   "left join exam_tag et on\n" +
                   "    et.id = eqt.exam_tag_id\n" +
                   "where\n" +
                   "    eq.id = :id\n" +
                   "group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, \n" +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3, \n" +
                   "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer, \n" +
                   "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by, \n" +
                   "    es.name, eq.level", nativeQuery = true)
    Optional<ExamQuestionDetailsRes> findOneById(String id);

    @Query(value = "select\n" +
                   "    eq.id,\n" +
                   "    eq.code,\n" +
                   "    eq.type,\n" +
                   "    eq.content,\n" +
                   "    eq.file_path as filePath,\n" +
                   "    eq.number_answer as numberAnswer,\n" +
                   "    eq.content_answer1 as contentAnswer1,\n" +
                   "    eq.content_answer2 as contentAnswer2,\n" +
                   "    eq.content_answer3 as contentAnswer3,\n" +
                   "    eq.content_answer4 as contentAnswer4,\n" +
                   "    eq.content_answer5 as contentAnswer5,\n" +
                   "    eq.multichoice,\n" +
                   "    eq.answer,\n" +
                   "    eq.explain,\n" +
                   "    eq.created_at as createdAt,\n" +
                   "    eq.updated_at as updatedAt,\n" +
                   "    eq.created_by as createdBy,\n" +
                   "    eq.updated_by as updatedBy,\n" +
                   "    es.name as examSubjectName,\n" +
                   "    eq.level,\n" +
                   "    string_agg(et.id, ',') as examTagIdStr,\n" +
                   "    string_agg(et.name, ',') as examTagNameStr\n" +
                   "from\n" +
                   "    exam_question eq\n" +
                   "left join exam_subject es on\n" +
                   "    es.id = eq.exam_subject_id\n" +
                   "left join exam_question_tag eqt on\n" +
                   "    eqt.exam_question_id = eq.id\n" +
                   "left join exam_tag et on\n" +
                   "    et.id = eqt.exam_tag_id\n" +
                   "where\n" +
                   "    eq.code = :code\n" +
                   "group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, \n" +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3, \n" +
                   "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer, \n" +
                   "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by, \n" +
                   "    es.name, eq.level", nativeQuery = true)
    Optional<ExamQuestionDetailsRes> findOneByCode(String code);

    List<ExamQuestionEntity> findAllByExamSubjectId(String examSubjectId);
}
