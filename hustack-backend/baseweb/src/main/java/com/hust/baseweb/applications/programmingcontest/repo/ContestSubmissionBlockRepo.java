package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ContestSubmissionBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContestSubmissionBlockRepo extends JpaRepository<ContestSubmissionBlock, UUID> {

    boolean existsBySubmissionId(UUID submissionId);

    List<ContestSubmissionBlock> findBySubmissionId(UUID submissionId);

}
