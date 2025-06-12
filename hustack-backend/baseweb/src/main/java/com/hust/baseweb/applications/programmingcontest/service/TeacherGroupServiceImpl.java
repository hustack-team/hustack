package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupService;
import com.hust.baseweb.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherGroupServiceImpl implements TeacherGroupService {

    TeacherGroupRepository teacherGroupRepository;
    TeacherGroupRelationRepository teacherGroupRelationRepository;
    UserService userService;

    @Override
    @Transactional
    public GroupMemberDTO createGroup(GroupMemberDTO groupDTO, String userId) throws IllegalArgumentException {
        TeacherGroup group = new TeacherGroup();
        group.setName(groupDTO.getName());
        group.setDescription(groupDTO.getDescription());
        group.setCreatedBy(userId);

        TeacherGroup savedGroup = create(group, userId, groupDTO.getUserIds() != null ? groupDTO.getUserIds() : Collections.emptyList());
        return convertToGroupDTO(savedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public AllGroupReponseDTO getGroup(UUID id, String userId) {
        TeacherGroup group = verifyGroupAccess(id, userId);
        return convertToAllGroupResponseDTO(group);
    }

    private AllGroupReponseDTO convertToAllGroupResponseDTO(TeacherGroup group) {
        AllGroupReponseDTO dto = new AllGroupReponseDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setCreatedBy(group.getCreatedBy());
        dto.setDescription(group.getDescription());
        dto.setLastModifiedDate(group.getLastModifiedDate());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModelSearchGroupResult> getGroups(GroupFilter filter, String userId) {
        filter.normalize();
        String keyword = StringUtils.isNotBlank(filter.getKeyword()) ? filter.getKeyword().trim() : null;

        int page = filter.getPage() < 0 ? 0 : filter.getPage();
        int size = filter.getSize() <= 0 ? 10 : filter.getSize();
        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> resultPage;
        if (keyword == null) {
            resultPage = teacherGroupRepository.findByCreatedByWithMemberCount(userId, pageable);
        } else {
            resultPage = teacherGroupRepository.findByUserIdAndNameContainingWithMemberCount(userId, keyword, pageable);
        }

        return resultPage.map(this::convertToModelSearchGroupResult);
    }

    private ModelSearchGroupResult convertToModelSearchGroupResult(Object[] result) {
        TeacherGroup group = (TeacherGroup) result[0];
        Long memberCount = (Long) result[1];

        ModelSearchGroupResult model = new ModelSearchGroupResult();
        model.setId(group.getId().toString());
        model.setName(group.getName());
        model.setDescription(group.getDescription());
        model.setCreatedBy(group.getCreatedBy());
        model.setLastModifiedDate(group.getLastModifiedDate());
        model.setMemberCount(memberCount.intValue());
        return model;
    }

    @Override
    @Transactional
    public GroupMemberDTO updateGroup(UUID id, GroupMemberDTO groupDTO, String userId) {
        TeacherGroup group = verifyGroupAccess(id, userId);

        group.setName(groupDTO.getName());
        group.setDescription(groupDTO.getDescription());

        TeacherGroup updatedGroup = teacherGroupRepository.save(group);

        return convertToGroupDTO(updatedGroup);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID id, String userId) {
        verifyGroupAccess(id, userId);
        deleteById(id);
    }

    @Override
    @Transactional
    public List<MemberDTO> addGroupMembers(UUID groupId, List<String> userIds, String userId) {
        verifyGroupAccess(groupId, userId);
        List<TeacherGroupRelation> relations = addMembers(groupId, userIds);
        List<String> relationUserIds = relations.stream().map(TeacherGroupRelation::getUserId).collect(Collectors.toList());
        Map<String, String> userFullNames = userService.getUserFullNames(relationUserIds);
        return relations.stream()
                        .map(relation -> convertToMemberDTO(relation, userFullNames))
                        .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberDTO> getGroupMembers(UUID groupId, String userId) {
        verifyGroupAccess(groupId, userId);
        List<TeacherGroupRelation> relations = teacherGroupRelationRepository.findByGroupId(groupId);
        List<String> relationUserIds = relations.stream().map(TeacherGroupRelation::getUserId).collect(Collectors.toList());
        Map<String, String> userFullNames = userService.getUserFullNames(relationUserIds);
        return relations.stream()
                        .map(relation -> convertToMemberDTO(relation, userFullNames))
                        .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberDTO getGroupMember(UUID groupId, String userId, String currentUserId) {
        verifyGroupAccess(groupId, currentUserId);
        TeacherGroupRelationId id = new TeacherGroupRelationId();
        id.setGroupId(groupId);
        id.setUserId(userId);
        TeacherGroupRelation relation = teacherGroupRelationRepository.findById(id)
                                                                      .orElseThrow(() -> new EntityNotFoundException("Member not found in group"));
        Map<String, String> userFullNames = userService.getUserFullNames(Collections.singletonList(userId));
        return convertToMemberDTO(relation, userFullNames);
    }

    @Override
    @Transactional
    public void removeGroupMember(UUID groupId, String userId, String currentUserId) {
        verifyGroupAccess(groupId, currentUserId);
        removeMember(groupId, userId);
    }

    private TeacherGroup verifyGroupAccess(UUID groupId, String currentUserId) {
        TeacherGroup group = teacherGroupRepository.findById(groupId)
                                                   .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + groupId));
        if (!group.getCreatedBy().equals(currentUserId)) {
            throw new SecurityException("User does not have permission to access this group");
        }
        return group;
    }

    @Override
    @Transactional
    public TeacherGroup create(TeacherGroup teacherGroup, String creatorUserId, List<String> userIds) {
        TeacherGroup savedGroup = teacherGroupRepository.save(teacherGroup);

        List<TeacherGroupRelation> relations = new ArrayList<>();
        TeacherGroupRelation creatorRelation = new TeacherGroupRelation();
        creatorRelation.setGroupId(savedGroup.getId());
        creatorRelation.setUserId(creatorUserId);
        relations.add(creatorRelation);

        if (userIds != null) {
            for (String userId : userIds) {
                if (!userId.equals(creatorUserId)) {
                    TeacherGroupRelation relation = new TeacherGroupRelation();
                    relation.setGroupId(savedGroup.getId());
                    relation.setUserId(userId);
                    relations.add(relation);
                }
            }
        }

        teacherGroupRelationRepository.saveAll(relations);
        return savedGroup;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeacherGroup> findById(UUID id) {
        return teacherGroupRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherGroup> findAll() {
        return teacherGroupRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        teacherGroupRelationRepository.deleteByGroupId(id);
        teacherGroupRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<TeacherGroupRelation> addMembers(UUID groupId, List<String> userIds) {
        List<TeacherGroupRelation> relations = new ArrayList<>();
        for (String userId : userIds) {
            TeacherGroupRelation relation = new TeacherGroupRelation();
            relation.setGroupId(groupId);
            relation.setUserId(userId);
            relations.add(relation);
        }
        return teacherGroupRelationRepository.saveAll(relations);
    }

    @Override
    @Transactional
    public void removeMember(UUID groupId, String userId) {
        TeacherGroupRelationId id = new TeacherGroupRelationId();
        id.setGroupId(groupId);
        id.setUserId(userId);
        teacherGroupRelationRepository.deleteById(id);
    }

    private GroupMemberDTO convertToGroupDTO(TeacherGroup group) {
        GroupMemberDTO dto = new GroupMemberDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedBy(group.getCreatedBy());
        dto.setLastModifiedDate(group.getLastModifiedDate());
        return dto;
    }

    private MemberDTO convertToMemberDTO(TeacherGroupRelation relation, Map<String, String> userFullNames) {
        MemberDTO dto = new MemberDTO();
        dto.setGroupId(relation.getGroupId());
        dto.setUserId(relation.getUserId());
        dto.setFullName(userFullNames.getOrDefault(relation.getUserId(), "Anonymous"));
        dto.setAddedTime(relation.getCreatedDate());
        return dto;
    }
}
