package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import com.hust.baseweb.applications.programmingcontest.entity.UserRegistrationContestEntity;
import com.hust.baseweb.applications.programmingcontest.model.ContestMembers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserRegistrationContestRepo extends JpaRepository<UserRegistrationContestEntity, UUID> {

    List<UserRegistrationContestEntity> findAllByContestId(String contestId);

    List<UserRegistrationContestEntity> findUserRegistrationContestEntityByContestIdAndUserId(
        String contestId,
        String userId
    );

    UserRegistrationContestEntity findUserRegistrationContestEntityByContestIdAndUserIdAndRoleId(
        String contestId,
        String userId,
        String roleId
    );

    List<UserRegistrationContestEntity> findByContestIdAndUserIdAndStatus(
        String contestId,
        String userId,
        String status
    );

    List<UserRegistrationContestEntity> findUserRegistrationContestEntityByContestIdAndUserIdAndStatusAndRoleId(
        String contestId,
        String userId,
        String status,
        String roleId
    );

    boolean existsByContestIdAndUserIdAndStatusAndRoleIdIn(
        String contestId,
        String userId,
        String status,
        List<String> roleIds
    );

    List<UserRegistrationContestEntity> findAllByUserIdAndRoleIdAndStatus(String userId, String roleId, String status);

    List<UserRegistrationContestEntity> findAllByUserIdAndRoleIdIn(String userId, List<String> roles);

    List<UserRegistrationContestEntity> findAllByContestIdAndStatus(String contestId, String status);

    @Query(value = "select " +
                   "    cast(user_registration_contest_id as varchar) as id, " +
//                   "    contest_id as contestId, " +
                   "    user_id as userId, " +
                   "    ul.first_name as firstName, " +
                   "    ul.last_name as lastName, " +
                   "    role_id as roleId, " +
                   "    updated_by_user_login_id as updatedByUserId, " +
                   "    last_updated as lastUpdatedDate, " +
                   "    permission_id as permissionId " +
                   "from " +
                   "    user_registration_contest_new urcn " +
                   "inner join user_login ul on " +
                   "    urcn.user_id = ul.user_login_id " +
                   "where " +
                   "    contest_id = ?1 " +
                   "    and status = ?2",
           nativeQuery = true)
    List<ContestMembers> findByContestIdAndStatus(String contestId, String status);

    @Query(value = "select * from user_registration_contest_new u " +
                   "where u.contest_id = ?1 " +
                   "and u.status = ?3 " +
                   " and u.user_id in (select participant_id from contest_user_participant_group where contest_id = ?1 and user_id = ?2) ",
           nativeQuery = true)
    List<UserRegistrationContestEntity> findAllInGroupByContestIdAndStatus(
        String contestId,
        String userId,
        String status
    );

    @Query(value = "select user_id from user_registration_contest_new " +
                   "where contest_id = ?1 " +
                   "and status = ?2",
           nativeQuery = true)
    List<String> getAllUserIdsInContest(String contestId, String status);

    @Query("select c from ContestEntity c " +
           "inner join UserRegistrationContestEntity urcn on c.contestId = urcn.contestId " +
           "where urcn.userId = ?1 " +
           "and urcn.roleId = ?2 " +
           "and urcn.status = ?3 " +
           "and c.statusId in ('OPEN', 'RUNNING', 'COMPLETED') " +
           "order by c.createdAt desc")
    List<ContestEntity> findRegisteredContestsForParticipant(String userId, String roleId, String status);

}
