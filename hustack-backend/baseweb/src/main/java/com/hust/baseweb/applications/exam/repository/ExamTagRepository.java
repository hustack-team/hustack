package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTagRepository extends JpaRepository<ExamTagEntity, String> {

    List<ExamTagEntity> findAllByOrderByNameAsc();

    Optional<ExamTagEntity> findByName(String name);
}
