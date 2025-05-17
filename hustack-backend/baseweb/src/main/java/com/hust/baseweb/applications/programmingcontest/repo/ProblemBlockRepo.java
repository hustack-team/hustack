package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProblemBlockRepo extends JpaRepository<ProblemBlock, UUID> {
    List<ProblemBlock> findByProblemId(String problemId);
    void deleteByProblemId(String problemId);
}
