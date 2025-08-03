package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.UserContestProblemRole;
import com.hust.baseweb.applications.programmingcontest.model.UserProblemRoleDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserContestProblemRoleRepo extends JpaRepository<UserContestProblemRole, UUID> {

    List<UserContestProblemRole> findAllByProblemIdAndUserId(String problemId, String userId);

    int deleteAllByProblemIdAndUserIdAndRoleId(String problemId, String userId, String roleId);

    boolean existsByProblemIdAndUserIdAndRoleId(String problemId, String userId, String roleId);

    @Query(value = "SELECT role_id FROM user_contest_problem_role WHERE problem_id = ?1 AND user_id = ?2",
           nativeQuery = true)
    List<String> getRolesByProblemIdAndUserId(String problemId, String userId);

    @Query("SELECT new com.hust.baseweb.applications.programmingcontest.model.UserProblemRoleDTO(" +
           "upr.userId, upr.problemId, CONCAT(ul.firstName, ' ', ul.lastName), upr.roleId) " +
           "FROM UserContestProblemRole upr " +
           "JOIN UserLogin ul ON upr.userId = ul.userLoginId " +
           "WHERE upr.problemId = :problemId")
    List<UserProblemRoleDTO> findAllByProblemIdWithFullName(@Param("problemId") String problemId);

    @Query("SELECT upr FROM UserContestProblemRole upr " +
           "WHERE upr.problemId = :problemId " +
           "AND upr.userId IN :userIds")
    List<UserContestProblemRole> findAllByProblemIdAndUserIdsIn(
        @Param("problemId") String problemId,
        @Param("userIds") List<String> userIds
    );

    @Query("SELECT upr.problemId, upr.roleId FROM UserContestProblemRole upr " +
           "WHERE upr.problemId IN :problemIds AND upr.userId = :userId")
    List<Object[]> findRolesByProblemIdsAndUserId(
        @Param("problemIds") List<String> problemIds,
        @Param("userId") String userId
    );
}
