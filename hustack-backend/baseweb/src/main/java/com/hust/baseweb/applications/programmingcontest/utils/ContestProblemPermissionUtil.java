package com.hust.baseweb.applications.programmingcontest.utils;

import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import com.hust.baseweb.applications.programmingcontest.entity.ContestProblem;
import com.hust.baseweb.applications.programmingcontest.entity.UserRegistrationContestEntity;
import com.hust.baseweb.applications.programmingcontest.repo.ContestProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ContestRepo;
import com.hust.baseweb.applications.programmingcontest.repo.UserRegistrationContestRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContestProblemPermissionUtil {

    ContestRepo contestRepo;

    ContestProblemRepo contestProblemRepo;

    UserRegistrationContestRepo userRegistrationContestRepo;

    public void checkContestAccess(String userId, String contestId) {
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        if (contest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contest not found");
        }

        List<UserRegistrationContestEntity> userRegs = userRegistrationContestRepo.findByContestIdAndUserIdAndStatus(
            contestId,
            userId,
            UserRegistrationContestEntity.STATUS_SUCCESSFUL);
        boolean isOwnerOrManager = userRegs.stream().anyMatch(reg ->
                                                                  UserRegistrationContestEntity.ROLE_OWNER.equals(reg.getRoleId()) ||
                                                                  UserRegistrationContestEntity.ROLE_MANAGER.equals(reg.getRoleId()));
        if (isOwnerOrManager) {
            return;
        }

        if (Boolean.TRUE.equals(contest.getContestPublic())) {
            if (!Set.of(
                        ContestEntity.CONTEST_STATUS_RUNNING,
                        ContestEntity.CONTEST_STATUS_COMPLETED)
                    .contains(contest.getStatusId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contest is not accessible");
            }
        } else {
            if (userRegs.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not belong to contest");
            }
            if (!Set
                .of(
                    ContestEntity.CONTEST_STATUS_OPEN,
                    ContestEntity.CONTEST_STATUS_RUNNING,
                    ContestEntity.CONTEST_STATUS_COMPLETED)
                .contains(contest.getStatusId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contest is not accessible");
            }
        }
    }

    private void checkProblemInContest(String contestId, String problemId) {
        ContestProblem cp = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
        if (cp == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem does not belong to contest");
        }
    }

    public void checkContestProblemAccess(
        String userId,
        String contestId,
        String problemId
    ) {
        checkContestAccess(userId, contestId);
        checkProblemInContest(contestId, problemId);

        if (ContestEntity.CONTEST_STATUS_OPEN.equals(contestRepo.findContestStatusById(contestId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Problem access not allowed when contest is OPEN");
        }
    }

    public void checkUserHasAnyRoleInContest(String userId, String contestId, Set<String> requiredRoles) {
        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        if (contest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contest not found");
        }

        if (CollectionUtils.isEmpty(requiredRoles)) {
            throw new IllegalArgumentException("Required roles must not be empty");
        }

        if (requiredRoles.contains(UserRegistrationContestEntity.ROLE_PARTICIPANT)
            && Boolean.TRUE.equals(contest.getContestPublic())) {
            return;
        }

        boolean hasRole = userRegistrationContestRepo.existsByContestIdAndUserIdAndStatusAndRoleIdIn(
            contestId,
            userId,
            UserRegistrationContestEntity.STATUS_SUCCESSFUL,
            new ArrayList<>(requiredRoles)
        );
        if (!hasRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have required role in contest");
        }
    }

    public void checkContestAccessAndHasAnyRole(
        String userId,
        String contestId,
        Set<String> requiredRoles
    ) {
        checkContestAccess(userId, contestId);
        checkUserHasAnyRoleInContest(userId, contestId, requiredRoles);
    }
} 
