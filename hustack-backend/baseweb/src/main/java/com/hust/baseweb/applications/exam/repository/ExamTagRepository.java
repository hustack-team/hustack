package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTagRepository extends JpaRepository<ExamTagEntity, String> {

    List<ExamTagEntity> findAllByOrderByNameAsc();

    Optional<ExamTagEntity> findByName(String name);

    @Query(value = "select\n" +
                   "    et.*\n" +
                   "from\n" +
                   "    exam_tag et\n" +
                   "left join exam_question_tag eqt on\n" +
                   "    et.id = eqt.exam_tag_id\n" +
                   "where\n" +
                   "    eqt.exam_question_id = :examQuestionId", nativeQuery = true)
    List<ExamTagEntity> findAllByExamQuestionId(@Param("examQuestionId") String examQuestionId);
}
