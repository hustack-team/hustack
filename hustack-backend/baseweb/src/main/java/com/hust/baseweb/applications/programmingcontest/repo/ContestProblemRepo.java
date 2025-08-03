package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.composite.CompositeContestProblemId;
import com.hust.baseweb.applications.programmingcontest.entity.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ContestProblemRepo extends JpaRepository<ContestProblem, CompositeContestProblemId> {

    List<ContestProblem> findAllByContestId(String contestId);

    List<ContestProblem> findAllByProblemId(String problemId);

    ContestProblem findByContestIdAndProblemId(String contestId, String problemId);

    ContestProblem findByContestIdAndProblemRecode(String contestId, String problemRecode);

    List<ContestProblem> findByContestIdAndProblemIdIn(String contestId, Set<String> problemIds);

    List<ContestProblem> findAllByContestIdAndSubmissionModeNot(String contestId, String submissionMode);

    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END " +
           "FROM ContestProblem cp " +
           "JOIN ContestEntity c ON cp.contestId = c.contestId " +
           "WHERE cp.problemId = :problemId " +
           "AND c.statusId != :statusId")
    boolean existsByProblemIdAndContestStatusNot(@Param("problemId") String problemId, @Param("statusId") String statusId);
}
