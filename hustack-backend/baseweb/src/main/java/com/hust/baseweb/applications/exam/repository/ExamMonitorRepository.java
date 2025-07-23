package com.hust.baseweb.applications.exam.repository;

import com.hust.baseweb.applications.exam.entity.ExamMonitorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamMonitorRepository extends JpaRepository<ExamMonitorEntity, String> {

    List<ExamMonitorEntity> findAllByExamResultId(String examResultId);
}
