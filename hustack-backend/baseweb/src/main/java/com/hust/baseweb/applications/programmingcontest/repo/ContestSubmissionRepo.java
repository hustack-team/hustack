package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.admin.dataadmin.education.model.ProgrammingContestSubmissionOM;
import com.hust.baseweb.applications.programmingcontest.entity.ContestSubmissionEntity;
import com.hust.baseweb.applications.programmingcontest.model.ModelProblemMaxSubmissionPoint;
import com.hust.baseweb.applications.programmingcontest.model.ModelSubmissionInfoRanking;
import com.hust.baseweb.model.SubmissionProjection;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ContestSubmissionRepo extends JpaRepository<ContestSubmissionEntity, UUID> {

//    long countAllByContestId(String contestId);
//
//    List<Integer> getListProblemSubmissionDistinctWithHighestScore(@Param("userLogin") UserLogin userLogin);

//    @Query(value =
//               "select user_submission_id as userId, sum(p) as point, email, first_name, middle_name, last_name from " +
//               "( select user_submission_id, problem_id , max(point) as p, ul.email as email, person.first_name as first_name, person.middle_name as middle_name, person.last_name as last_name " +
//               "from contest_submission_new csn " +
//               "inner join user_login ul " +
//               "on user_submission_id in (select user_id from user_registration_contest_new urcn where urcn.contest_id=:contest_id and status='SUCCESSFUL') " +
//               "and csn.contest_id=:contest_id " +
//               "and csn.user_submission_id = ul.user_login_id " +
//               "inner join person  " +
//               "on person.party_id = ul.party_id " +
//               "group by problem_id, user_submission_id, ul.email, problem_id, user_submission_id, person.first_name, person.middle_name, person.last_name) " +
//               "as cur group by user_submission_id, email, first_name, middle_name, last_name order by point desc ",
//           nativeQuery = true)
//    List<Object[]> calculatorContest(@Param("contest_id") String contest_id);

    @Query(value = "select distinct problem_id from contest_submission_new " +
                   "where user_submission_id = :userId " +
                   "and contest_id = :contestId " +
                   "and status = 'Accepted'",
           nativeQuery = true)
    List<String> findAcceptedProblemsInContestOfUser(
        @Param("contestId") String contestId,
        @Param("userId") String userId
    );

    @Query(value = "select problem_id as problemId, max(point) as maxPoint from contest_submission_new " +
                   "where user_submission_id = :userId " +
                   "and contest_id = :contestId " +
                   "group by problemId ",
           nativeQuery = true)
    List<ModelProblemMaxSubmissionPoint> findProblemsInContestHasSubmissionOfUser(
        @Param("contestId") String contestId,
        @Param("userId") String userId
    );

    ContestSubmissionEntity findContestSubmissionEntityByContestSubmissionId(UUID contestSubmissionId);

    @Query(value = "select * " +
                   "from contest_submission_new csn " +
                   "where csn.contest_id = :cid and csn.user_submission_id = :uid and csn.problem_id = :pid " +
                   "order by created_stamp desc ",
           nativeQuery = true)
    List<ContestSubmissionEntity> findAllByContestIdAndUserIdAndProblemId(
        @Param("cid") String cid,
        @Param("uid") String uid,
        @Param("pid") String pid
    );

    @Query(value = "select * " +
                   "from contest_submission_new csn " +
                   "where csn.contest_id = :cid and csn.problem_id = :pid " +
                   "order by created_stamp desc ",
           nativeQuery = true)
    List<ContestSubmissionEntity> findAllByContestIdAndProblemId(
        @Param("cid") String cid,
        @Param("pid") String pid
    );

    @Query(value = "select count(*) " +
                   "from contest_submission_new csn " +
                   "where csn.contest_id = :cid and csn.user_submission_id = :uid and csn.problem_id = :pid",
           nativeQuery = true)
    int countAllByContestIdAndUserIdAndProblemId(
        @Param("cid") String cid,
        @Param("uid") String uid,
        @Param("pid") String pid
    );

    @Query(value = "select * " +
                   "from contest_submission_new csn " +
                   "where csn.contest_id = :cid and csn.status = :status " +
                   "order by created_stamp asc ",
           nativeQuery = true)
    List<ContestSubmissionEntity> findAllByContestIdAndStatus(@Param("cid") String cid, @Param("status") String status);

//    List<ContestSubmissionEntity> findAllByStatus(@Param("status") String status);

    @Query(value = "select * " +
                   "from contest_submission_new " +
                   "where status = ?3 " +
                   "order by created_stamp desc " +
                   "offset ?1 limit ?2",
           nativeQuery = true)
    List<ContestSubmissionEntity> getPageContestSubmission(int offset, int limit, String status);

    List<ContestSubmissionEntity> findAllByContestId(String contestId);

//    void deleteAllByContestId(String contestId);

    @Query(value = "select count(*) from contest_submission_new where status = 'Accepted'", nativeQuery = true)
    int countTotalAccept();

    @Query(nativeQuery = true,
           value =
               "SELECT ct.contest_id contestId, ct.contest_name contestName, p.problem_id problemId, p.problem_name problemName, " +
               "CAST(cts.contest_submission_id as VARCHAR(64)) submissionId, cts.status status, cts.test_case_pass testCasePass, cts.point point, " +
               "cts.source_code_language sourceCodeLanguage, cts.created_stamp submitAt " +
               "FROM contest_submission_new cts " +
               "INNER JOIN contest_new ct ON cts.user_submission_id = :studentLoginId AND cts.contest_id = ct.contest_id " +
               "INNER JOIN contest_problem_new p ON cts.problem_id = p.problem_id " +
               "WHERE LOWER(ct.contest_name) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(p.problem_name) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(CAST(cts.contest_submission_id as VARCHAR)) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(cts.status) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(cts.test_case_pass) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(CAST(cts.point as VARCHAR(10))) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(cts.source_code_language) LIKE CONCAT('%', LOWER(:search), '%') " +
               "OR LOWER(CAST(cts.created_stamp as VARCHAR)) LIKE CONCAT('%', LOWER(:search), '%')")
    Page<ProgrammingContestSubmissionOM> findContestSubmissionsOfStudent(
        @Param("studentLoginId") String studentLoginId,
        @Param("search") String search,
        Pageable pageable
    );

    @Modifying
    @Query("update ContestSubmissionEntity s set s.status = ?2 where s.contestSubmissionId = ?1")
    void updateContestSubmissionStatus(UUID contestSubmissionId, String status);

    @Query(value = "SELECT problem_id AS problemId, MAX(point) AS maxPoint " +
                   "FROM contest_submission_new " +
                   "WHERE user_submission_id = :userId " +
                   "AND contest_id = :contestId " +
                   "AND final_selected_submission = 1 " +
                   "GROUP BY problem_id",
           nativeQuery = true)
    List<ModelProblemMaxSubmissionPoint> findProblemsInContestHasFinalSelectedSubmissionOfUser(
        @Param("contestId") String contestId,
        @Param("userId") String userId
    );

    @Query(value =
        "select new com.hust.baseweb.applications.programmingcontest.model.ModelSubmissionInfoRanking(c.problemId, MAX(c.point), 0.0) " +
        "from ContestSubmissionEntity c " +
        "where c.userId = ?1 " +
        "and c.contestId = ?2 " +
        "and (c.managementStatus is null or c.managementStatus != 'DISABLED') " +
        "group by c.problemId")
    List<ModelSubmissionInfoRanking> getHighestSubmissions(String userId, String contestId);

    @Query(value =
        "select new com.hust.baseweb.applications.programmingcontest.model.ModelSubmissionInfoRanking(c.problemId, c.point, 0.0) " +
        "from ContestSubmissionEntity c " +
        "where c.userId = ?1 " +
        "and c.contestId = ?2 " +
        "and c.createdAt = (select c2.createdAt " +
        "                    from ContestSubmissionEntity c2 " +
        "                    where c.createdAt = c2.createdAt)")
    List<ModelSubmissionInfoRanking> getLatestSubmissions(String userId, String contestId);

    @Query(value =
        "select new com.hust.baseweb.applications.programmingcontest.model.ModelSubmissionInfoRanking(c.problemId, MAX(c.point), 0.0D) " +
        "from ContestSubmissionEntity c " +
        "where c.userId = ?1 " +
        "and c.contestId = ?2 " +
        "and c.finalSelectedSubmission = 1 " +
        "and (c.managementStatus is null or c.managementStatus != 'DISABLED') " +
        "group by c.problemId")
    List<ModelSubmissionInfoRanking> getHighestPinnedSubmissions(String userId, String contestId);

    @Query(value =
        "select new com.hust.baseweb.applications.programmingcontest.model.ModelSubmissionInfoRanking(c.problemId, c.point, 0.0D) " +
        "from ContestSubmissionEntity c " +
        "where c.userId = ?1 " +
        "and c.contestId = ?2 " +
        "and c.finalSelectedSubmission = 1 " +
        "and (c.managementStatus is null or c.managementStatus != 'DISABLED') " +
        "and c.createdAt = (select c2.createdAt " +
        "                    from ContestSubmissionEntity c2 " +
        "                    where c.createdAt = c2.createdAt)")
    List<ModelSubmissionInfoRanking> getLatestPinnedSubmissions(String userId, String contestId);

    @Query(value = "select " +
                   "    cast(csn.contest_submission_id as varchar) contestSubmissionId, " +
                   "    csn.problem_id as problemId, " +
                   "    csn.contest_id as contestId, " +
                   "    csn.submitted_by_user_id as userId, " +
                   "    concat(ul.first_name, ' ', ul.last_name) as fullName, " +
                   "    csn.test_case_pass as testCasePass, " +
                   "    csn.source_code_language as sourceCodeLanguage, " +
                   "    csn.created_stamp as createdAt, " +
                   "    csn.status as status, " +
                   "    csn.created_by_ip as createdByIp " +
                   "from " +
                   "    contest_submission_new csn " +
                   "left join user_login ul on " +
                   "    csn.submitted_by_user_id = ul.user_login_id " +
                   "where " +
                   "    (:statuses is null or csn.status = any (string_to_array(cast(:statuses as text), ','))) " +
                   "    and (:userId is null or upper(csn.submitted_by_user_id) like upper(concat('%', :userId, '%'))) " +
                   "    and (:problemId is null or upper(csn.problem_id) like upper(concat('%', :problemId, '%'))) " +
                   "    and (cast(cast(:fromDate as text) as timestamp) is null or csn.created_stamp >= cast(cast(:fromDate as text) as timestamp)) " +
                   "    and (cast(cast(:toDate as text) as timestamp) is null or csn.created_stamp <= cast(cast(:toDate as text) as timestamp)) " +
                   "    and (:languages is null or csn.source_code_language = any (string_to_array(cast(:languages as text), ','))) " +
                   "    and (:contestId is null or upper(csn.contest_id) like upper(concat('%', :contestId, '%'))) ",
           countQuery = "select " +
                        "    count(contest_submission_id) " +
                        "from " +
                        "    contest_submission_new csn " +
                        "where " +
                        "    (:statuses is null or csn.status = any (string_to_array(cast(:statuses as text), ','))) " +
                        "    and (:userId is null or upper(csn.submitted_by_user_id) like upper(concat('%', :userId, '%'))) " +
                        "    and (:problemId is null or upper(csn.problem_id) like upper(concat('%', :problemId, '%'))) " +
                        "    and (cast(cast(:fromDate as text) as timestamp) is null or csn.created_stamp >= cast(cast(:fromDate as text) as timestamp)) " +
                        "    and (cast(cast(:toDate as text) as timestamp) is null or csn.created_stamp <= cast(cast(:toDate as text) as timestamp)) " +
                        "    and (:languages is null or csn.source_code_language = any (string_to_array(cast(:languages as text), ','))) " +
                        "    and (:contestId is null or upper(csn.contest_id) like upper(concat('%', :contestId, '%'))) ",
           nativeQuery = true)
    Page<SubmissionProjection> findAllBy(
        @Param("userId") String userId,
        @Param("contestId") String contestId,
        @Param("problemId") String problemId,
        @Param("languages") String languages,
        @Param("statuses") String statuses,
        @Param("fromDate") ZonedDateTime fromDate,
        @Param("toDate") ZonedDateTime toDate,
        Pageable pageable
    );

    @Query(
        "SELECT CASE WHEN COUNT(cs.contestSubmissionId) > 0 THEN true ELSE false END FROM ContestSubmissionEntity cs WHERE cs.problemId = :problemId")
    boolean existsByProblemId(@Param("problemId") String problemId);

    @Query(value = "select " +
                   "    case " +
                   "        when COUNT(cs.contest_submission_id) > 0 then true " +
                   "        else false " +
                   "    end " +
                   "from " +
                   "    contest_submission_new cs " +
                   "inner join contest_contest_problem_new cp on " +
                   "    cs.contest_id = cp.contest_id and cs.problem_id = cp.problem_id " +
                   "inner join contest_new c on " +
                   "    cp.contest_id = c.contest_id " +
                   "left join user_registration_contest_new urc on " +
                   "    cs.user_submission_id = urc.user_id and cs.contest_id = urc.contest_id " +
                   "where " +
                   "    cs.problem_id = :problemId " +
                   "    and cs.user_submission_id != c.user_create_id " +
                   "    and (urc.role_id is null or urc.role_id not in (:excludedRoles)) ",
           nativeQuery = true)
    boolean existsByProblemIdAndRoleNotIn(
        @Param("problemId") String problemId,
        @Param("excludedRoles") Set<String> excludedRoles
    );
}
