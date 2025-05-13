package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemBlockRepo extends JpaRepository<ProblemBlock, Long> {
    List<ProblemBlock> findByProblemId(String problemId);
    void deleteByProblemId(String problemId);
}
