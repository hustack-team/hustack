package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface ContestRepo extends JpaRepository<ContestEntity, String> {

    ContestEntity findContestByContestId(String contestId);

    @Query("select c.statusId from ContestEntity c where c.contestId = ?1")
    String findContestStatusById(String contestId);

    List<ContestEntity> findByContestIdInAndStatusIdNot(Set<String> ids, String statusId);

    @Modifying
    @Query(value = "update contest_new " +
                   "set judge_mode = :judgeMode ",
           nativeQuery = true
    )
    void switchAllContestToJudgeMode(String judgeMode);

    List<ContestEntity> findByContestPublicTrue();

    @Query(value = "select * from contest_new " +
                   "where public = true " +
                   "and status_id in ('RUNNING', 'COMPLETED') " +
                   "order by created_stamp desc",
           nativeQuery = true)
    List<ContestEntity> findPublicContestsForParticipant();

}
