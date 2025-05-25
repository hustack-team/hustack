package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.model.ModelSearchGroupResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherGroupService {
    TeacherGroup create(TeacherGroup teacherGroup, String creatorUserId, List<String> userIds);
    Optional<TeacherGroup> findById(UUID id);
    List<TeacherGroup> findAll();
    TeacherGroup update(UUID id, TeacherGroup updatedGroup);
    void deleteById(UUID id);
    List<TeacherGroupRelation> addMembers(UUID groupId, List<String> userIds);
    void removeMember(UUID groupId, String userId);

    Page<ModelSearchGroupResult> search(
        String keyword,
        List<String> excludeIds,
        Pageable pageable,
        String userId
    );
}
