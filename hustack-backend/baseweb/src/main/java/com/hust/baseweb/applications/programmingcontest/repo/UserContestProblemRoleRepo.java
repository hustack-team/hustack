package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.UserContestProblemRole;
import com.hust.baseweb.applications.programmingcontest.model.ModelResponseUserProblemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserContestProblemRoleRepo extends JpaRepository<UserContestProblemRole, UUID> {

    List<UserContestProblemRole> findAllByProblemId(String problemId);

    List<UserContestProblemRole> findAllByProblemIdAndUserId(String problemId, String userId);

    List<UserContestProblemRole> findAllByProblemIdAndUserIdAndRoleId(String problemId, String userId, String roleId);

    /*
    @Query(value = "SELECT DISTINCT problem_id FROM user_contest_problem_role WHERE user_id = ?1 AND problem_id NOT IN"
            +
            "(SELECT DISTINCT problem_id FROM user_contest_problem_role WHERE user_id = ?1 AND role_id = 'OWNER')", nativeQuery = true)
    */

    boolean existsByProblemIdAndUserIdAndRoleId(String problemId, String userId, String roleId);

    @Query(value = "SELECT role_id FROM user_contest_problem_role WHERE problem_id = ?1 AND user_id = ?2", nativeQuery = true)
    List<String> getRolesByProblemIdAndUserId(String problemId, String userId);

    @Query("SELECT new com.hust.baseweb.applications.programmingcontest.model.ModelResponseUserProblemRole(" +
           "upr.userId, upr.problemId, CONCAT(ul.firstName, ' ', ul.lastName), upr.roleId) " +
           "FROM UserContestProblemRole upr " +
           "JOIN UserLogin ul ON upr.userId = ul.userLoginId " +
           "WHERE upr.problemId = :problemId")
    List<ModelResponseUserProblemRole> findAllByProblemIdWithFullName(@Param("problemId") String problemId);
}
