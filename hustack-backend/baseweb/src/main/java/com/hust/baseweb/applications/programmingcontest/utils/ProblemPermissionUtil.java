package com.hust.baseweb.applications.programmingcontest.utils;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.entity.UserContestProblemRole;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.UserContestProblemRoleRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProblemPermissionUtil {

    ProblemRepo problemRepo;

    UserContestProblemRoleRepo userContestProblemRoleRepo;

    /**
     * Check view permission based on permission matrix
     *
     * @param problemId Problem ID to check
     * @param userId    User ID
     * @return true if it has view permission, false otherwise
     */
    public boolean checkViewProblemPermission(String problemId, String userId) {
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (problem == null) {
            return false;
        }

        String status = problem.getStatusId();
        if (ProblemEntity.PROBLEM_STATUS_OPEN.equals(status) && problem.isPublicProblem()) {
            return true;
        }

        if (problem.getCreatedBy().equals(userId)) {
            return true;
        }

        List<String> userRoles = userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problemId, userId);
        if (!CollectionUtils.isEmpty(userRoles)) {
            if (ProblemEntity.PROBLEM_STATUS_OPEN.equals(status)) {
                return userRoles
                    .stream()
                    .anyMatch(role -> UserContestProblemRole.ROLE_OWNER.equals(role) ||
                                      UserContestProblemRole.ROLE_EDITOR.equals(role) ||
                                      UserContestProblemRole.ROLE_VIEWER.equals(role)
                    );
            } else if (ProblemEntity.PROBLEM_STATUS_HIDDEN.equals(status)) {
                return userRoles.stream().anyMatch(role -> UserContestProblemRole.ROLE_OWNER.equals(role) ||
                                                           UserContestProblemRole.ROLE_EDITOR.equals(role));
            }
        }

        return false;
    }

    /**
     * Check edit permission based on permission matrix
     *
     * @param problemId Problem ID to check
     * @param userId    User ID
     * @return true if it has edit permission, false otherwise
     */
    public boolean checkEditProblemPermission(String problemId, String userId) {
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (problem == null) {
            return false;
        }

        if (problem.getCreatedBy().equals(userId)) {
            return true;
        }

        List<String> userRoles = userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problemId, userId);
        return !CollectionUtils.isEmpty(userRoles) &&
               userRoles.stream().anyMatch(role -> UserContestProblemRole.ROLE_OWNER.equals(role) ||
                                                   UserContestProblemRole.ROLE_EDITOR.equals(role));
    }

    public void checkProblemOwnerPermission(String problemId, String userId) {
        if (!problemRepo.existsByProblemId(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }

        boolean isOwner = userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(
            problemId,
            userId,
            UserContestProblemRole.ROLE_OWNER);
        if (!isOwner) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }
    }
} 
