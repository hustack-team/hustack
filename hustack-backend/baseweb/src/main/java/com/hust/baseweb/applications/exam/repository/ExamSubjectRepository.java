package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamSubjectEntity;
import com.hust.baseweb.applications.exam.utils.Constants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubjectRepository extends JpaRepository<ExamSubjectEntity, String> {

    Optional<ExamSubjectEntity> findByCode(String code);

    List<ExamSubjectEntity> findAllByStatusOrderByName(Constants.Status status);
}
