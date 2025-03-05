package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamQuestionEntity;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionDetailsRes;
import com.hust.baseweb.applications.exam.model.response.ExamQuestionFilterRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = "select " +
                   "    eq.*, " +
                   "    es.id as examSubjectId, " +
                   "    es.name as examSubjectName, " +
                   "    COALESCE(json_agg(json_build_object('id', et.id, 'name', et.name)) FILTER (WHERE et.id IS NOT NULL), '[]') AS examTags " +
                   "from " +
                   "    exam_question eq " +
                   "left join exam_subject es on " +
                   "    es.id = eq.exam_subject_id " +
                   "left join exam_question_tag eqt on " +
                   "    eq.id = eqt.exam_question_id " +
                   "left join exam_tag et on " +
                   "    et.id = eqt.exam_tag_id " +
                   "where " +
                   "    eq.created_by = :userLogin " +
                   "    and es.status = 'ACTIVE' " +
                   "and " +
                   "    (:type is null or eq.type = :type) " +
                   "and " +
                   "    (:examTagIds is null or eqt.exam_tag_id in (:examTagIds)) " +
                   "and " +
                   "    (:level is null or eq.level = :level) " +
                   "and " +
                   "    (:examSubjectId is null or eq.exam_subject_id = :examSubjectId) " +
                   "and  " +
                   "    (:keyword is null or " +
                   "    (lower(eq.code) like CONCAT('%', lower(:keyword),'%')) or " +
                   "    (lower(eq.content) like CONCAT('%', lower(:keyword),'%'))) " +
                   "group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, " +
                   "    eq.content_answer1, eq.content_answer2, eq.content_answer3, " +
                   "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer, " +
                   "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by, " +
                   "    eq.exam_subject_id, eq.level ,es.id, es.name " +
                   "order by eq.created_at desc",
           countQuery = "select " +
                        "    count(1) " +
                        "from " +
                        "    exam_question eq " +
                        "left join exam_subject es on " +
                        "    es.id = eq.exam_subject_id " +
                        "left join exam_question_tag eqt on " +
                        "    eq.id = eqt.exam_question_id " +
                        "left join exam_tag et on " +
                        "    et.id = eqt.exam_tag_id " +
                        "where " +
                        "    eq.created_by = :userLogin " +
                        "    and es.status = 'ACTIVE' " +
                        "and " +
                        "    (:type is null or eq.type = :type) " +
                        "and " +
                        "    (:examTagIds is null or eqt.exam_tag_id in (:examTagIds)) " +
                        "and " +
                        "    (:level is null or eq.level = :level) " +
                        "and " +
                        "    (:examSubjectId is null or eq.exam_subject_id = :examSubjectId) " +
                        "and  " +
                        "    (:keyword is null or " +
                        "    (lower(eq.code) like CONCAT('%', lower(:keyword),'%')) or " +
                        "    (lower(eq.content) like CONCAT('%', lower(:keyword),'%'))) " +
                        "group by eq.id, eq.code, eq.type, eq.content, eq.file_path, eq.number_answer, " +
                        "    eq.content_answer1, eq.content_answer2, eq.content_answer3, " +
                        "    eq.content_answer4, eq.content_answer5, eq.multichoice, eq.answer, " +
                        "    eq.explain, eq.created_at, eq.updated_at, eq.created_by, eq.updated_by, " +
                        "    eq.exam_subject_id, eq.level ,es.id, es.name ",
           nativeQuery = true)
    Page<ExamQuestionFilterRes> filter(
        Pageable pageable,
        @Param("userLogin") String userLogin,
        @Param("type") Integer type,
        @Param("examTagIds") List<String> examTagIds,
        @Param("level") String level,
        @Param("examSubjectId") String examSubjectId,
        @Param("keyword") String keyword
    );
}
