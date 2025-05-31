package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.model.*;
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

    GroupMemberDTO createGroup(GroupMemberDTO groupDTO, String userId) throws IllegalArgumentException;
    AllGroupReponseDTO getGroup(UUID id, String userId);
    GroupMemberDTO updateGroup(UUID id, GroupMemberDTO groupDTO, String userId);
    void deleteGroup(UUID id, String userId);
    List<MemberDTO> addGroupMembers(UUID groupId, List<String> userIds, String userId);
    List<MemberDTO> getGroupMembers(UUID groupId, String userId);
    MemberDTO getGroupMember(UUID groupId, String userId, String currentUserId);
    void removeGroupMember(UUID groupId, String userId, String currentUserId);
    Page<ModelSearchGroupResult> getGroups(GroupFilter filter, String userId);
}
