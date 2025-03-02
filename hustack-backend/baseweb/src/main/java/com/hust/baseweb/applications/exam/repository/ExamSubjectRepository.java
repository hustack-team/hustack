package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamSubjectEntity;
import com.hust.baseweb.applications.exam.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubjectRepository extends JpaRepository<ExamSubjectEntity, String> {

    Optional<ExamSubjectEntity> findByCode(String code);

    List<ExamSubjectEntity> findAllByStatusOrderByName(Constants.Status status);

    @Query(value = "select " +
                   "    * " +
                   "from " +
                   "    exam_subject es " +
                   "where " +
                   "    es.created_by = :userLogin " +
                   "and " +
                   "    (:status is null or es.status = :status) " +
                   "and " +
                   "    (:keyword is null or " +
                   "    (lower(es.code) like CONCAT('%', lower(:keyword),'%')) or  " +
                   "    (lower(es.name) like CONCAT('%', lower(:keyword),'%')))  " +
                   "order by es.name asc",
           countQuery = "select " +
                        "    count(1) " +
                        "from " +
                        "    exam_subject es " +
                        "where " +
                        "    es.created_by = :userLogin " +
                        "and " +
                        "    (:status is null or es.status = :status) " +
                        "and " +
                        "    (:keyword is null or " +
                        "    (lower(es.code) like CONCAT('%', lower(:keyword),'%')) or " +
                        "    (lower(es.name) like CONCAT('%', lower(:keyword),'%'))) ",
           nativeQuery = true)
    Page<ExamSubjectEntity> filter(
        Pageable pageable,
        @Param("userLogin") String userLogin,
        @Param("status") String status,
        @Param("keyword") String keyword
    );
}
