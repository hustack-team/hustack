package com.hust.baseweb.applications.examclassandaccount.repo;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ExamClassUserloginMapRepo extends JpaRepository<ExamClassUserloginMap, UUID> {

    @Query("SELECT e FROM ExamClassUserloginMap e " +
           "WHERE e.examClassId = :examClassId " +
           "ORDER BY e.orderIndex ASC")
    List<ExamClassUserloginMap> findByExamClassId(UUID examClassId);

    List<ExamClassUserloginMap> findByExamClassIdAndStatus(UUID examClassId, String status);

    int deleteByExamClassIdAndStatusIsNot(UUID examClassId, String status);

    ExamClassUserloginMap findByExamClassIdAndId(UUID examClassId, UUID id);
}
