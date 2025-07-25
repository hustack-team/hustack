package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeacherGroupRepository extends JpaRepository<TeacherGroup, UUID> {
    Page<TeacherGroup> findByCreatedBy(String createdBy, Pageable pageable);

    @Query("SELECT g FROM TeacherGroup g WHERE g.createdBy = :userId " +
           "AND (:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
    Page<TeacherGroup> findByUserIdAndNameContaining(
        @Param("userId") String userId,
        @Param("keyword") String keyword,
        Pageable pageable);

    @Query("SELECT g, COUNT(tgr) as memberCount " +
           "FROM TeacherGroup g " +
           "LEFT JOIN TeacherGroupRelation tgr ON tgr.groupId = g.id " +
           "WHERE g.createdBy = :userId " +
           "GROUP BY g")
    Page<Object[]> findByCreatedByWithMemberCount(
        @Param("userId") String userId,
        Pageable pageable);

    @Query("SELECT g, COUNT(tgr) as memberCount " +
           "FROM TeacherGroup g " +
           "LEFT JOIN TeacherGroupRelation tgr ON tgr.groupId = g.id " +
           "WHERE g.createdBy = :userId " +
           "AND (:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "GROUP BY g")
    Page<Object[]> findByUserIdAndNameContainingWithMemberCount(
        @Param("userId") String userId,
        @Param("keyword") String keyword,
        Pageable pageable);

}
