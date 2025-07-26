package com.hust.baseweb.applications.examclassandaccount.repo;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ExamAccountRepo extends JpaRepository<ExamAccount, UUID> {

    @Query("SELECT e FROM ExamAccount e " +
           "WHERE e.examClassId = :examClassId " +
           "ORDER BY e.orderIndex ASC")
    List<ExamAccount> findByExamClassId(UUID examClassId);

    List<ExamAccount> findByExamClassIdAndStatus(UUID examClassId, String status);

    int deleteByExamClassIdAndStatusIsNot(UUID examClassId, String status);

    ExamAccount findByExamClassIdAndId(UUID examClassId, UUID id);
}
