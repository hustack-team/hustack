package com.hust.baseweb.applications.programmingcontest.repo;


import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeacherGroupRelationRepository extends JpaRepository<TeacherGroupRelation, TeacherGroupRelationId> {
    @Query("SELECT tgr.groupId FROM TeacherGroupRelation tgr WHERE tgr.userId LIKE %:keyword%")
    List<UUID> findGroupIdsByUserIdContaining(@Param("keyword") String keyword);
    List<TeacherGroupRelation> findByGroupId(UUID groupId);

    @Query("SELECT tgr.userId FROM TeacherGroupRelation tgr WHERE tgr.groupId IN :groupIds")
    List<String> findUserIdsByGroupIds(@Param("groupIds") List<UUID> groupIds);
    @Query("SELECT r.groupId FROM TeacherGroupRelation r " +
           "WHERE r.userId = :userId")
    List<UUID> findGroupIdsByUserId(@Param("userId") String userId);
}
