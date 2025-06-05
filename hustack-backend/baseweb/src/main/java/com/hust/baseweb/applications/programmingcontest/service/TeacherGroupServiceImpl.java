package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupService;
import com.hust.baseweb.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
//        group.setStatus(groupDTO.getStatus());
        group.setDescription(groupDTO.getDescription());
        group.setCreatedBy(userId);

        TeacherGroup savedGroup = create(group, userId, groupDTO.getUserIds() != null ? groupDTO.getUserIds() : Collections.emptyList());
        return convertToGroupDTO(savedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public AllGroupReponseDTO getGroup(UUID id, String userId) {
        TeacherGroup group = teacherGroupRepository.findById(id)
                                                   .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + id));
        if (!group.getCreatedBy().equals(userId)) {
            throw new SecurityException("User does not have permission to access this group");
        }
        return convertToAllGroupResponseDTO(group);
    }



    private AllGroupReponseDTO convertToAllGroupResponseDTO(TeacherGroup group) {
        AllGroupReponseDTO dto = new AllGroupReponseDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
//        dto.setStatus(group.getStatus());
        dto.setCreatedBy(group.getCreatedBy());
        dto.setDescription(group.getDescription());
        dto.setLastModifiedDate(group.getLastModifiedDate());
        return dto;
    }

    @Override
    public Page<ModelSearchGroupResult> getGroups(GroupFilter filter, String userId) {
        filter.normalize();
        String keyword = StringUtils.isNotBlank(filter.getKeyword()) ? filter.getKeyword().trim() : null;

        int page = filter.getPage() < 0 ? 0 : filter.getPage();
        int size = filter.getSize() <= 0 ? 10 : filter.getSize();
        Pageable pageable = PageRequest.of(page, size);

        Page<TeacherGroup> teacherGroups;
        if (keyword == null) {
            teacherGroups = teacherGroupRepository.findByCreatedBy(userId, pageable);
        } else {
            teacherGroups = teacherGroupRepository.findByUserIdAndNameContainingAndNotInExcludeIds(
                userId, keyword != null ? keyword : "", pageable);
        }

        return teacherGroups.map(this::convertToModelSearchGroupResult);
    }

    private ModelSearchGroupResult convertToModelSearchGroupResult(TeacherGroup group) {
        ModelSearchGroupResult result = new ModelSearchGroupResult();
        result.setId(group.getId().toString());
        result.setName(group.getName());
        result.setDescription(group.getDescription());
        result.setCreatedBy(group.getCreatedBy());
        result.setLastModifiedDate(group.getLastModifiedDate());
        result.setMemberCount(teacherGroupRelationRepository.countByGroupId(group.getId()));
        return result;
    }

    @Override
    @Transactional
    public GroupMemberDTO updateGroup(UUID id, GroupMemberDTO groupDTO, String userId) {
        TeacherGroup group = findById(id)
            .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + id));
        if (!group.getCreatedBy().equals(userId)) {
            throw new SecurityException("User does not have permission to update this group");
        }
        group.setName(groupDTO.getName());
//        group.setStatus(groupDTO.getStatus());
        group.setDescription(groupDTO.getDescription());

        TeacherGroup updatedGroup = update(id, group);
        return convertToGroupDTO(updatedGroup);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID id, String userId) {
        TeacherGroup group = findById(id)
            .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + id));
        if (!group.getCreatedBy().equals(userId)) {
            throw new SecurityException("User does not have permission to delete this group");
        }
        deleteById(id);
    }

    @Override
    @Transactional
    public List<MemberDTO> addGroupMembers(UUID groupId, List<String> userIds, String userId) {
        TeacherGroup group = findById(groupId)
            .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + groupId));
        if (!group.getCreatedBy().equals(userId)) {
            throw new SecurityException("User does not have permission to modify this group");
        }
        List<TeacherGroupRelation> relations = addMembers(groupId, userIds);
        return relations.stream()
                        .map(this::convertToMemberDTO)
                        .collect(Collectors.toList());
    }

    @Override
    public List<MemberDTO> getGroupMembers(UUID groupId, String userId) {
        TeacherGroup group = findById(groupId)
            .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + groupId));
        if (!group.getCreatedBy().equals(userId)) {
            throw new SecurityException("User does not have permission to access this group");
        }
        return teacherGroupRelationRepository.findAll()
                                             .stream()
                                             .filter(relation -> relation.getGroupId().equals(groupId))
                                             .map(this::convertToMemberDTO)
                                             .collect(Collectors.toList());
    }

    @Override
    public MemberDTO getGroupMember(UUID groupId, String userId, String currentUserId) {
        TeacherGroup group = findById(groupId)
            .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + groupId));
        if (!group.getCreatedBy().equals(currentUserId)) {
            throw new SecurityException("User does not have permission to access this group");
        }
        TeacherGroupRelationId id = new TeacherGroupRelationId();
        id.setGroupId(groupId);
        id.setUserId(userId);
        TeacherGroupRelation relation = teacherGroupRelationRepository.findById(id)
                                                                      .orElseThrow(() -> new EntityNotFoundException("Member not found in group"));
        return convertToMemberDTO(relation);
    }

    @Override
    @Transactional
    public void removeGroupMember(UUID groupId, String userId, String currentUserId) {
        TeacherGroup group = findById(groupId)
            .orElseThrow(() -> new EntityNotFoundException("TeacherGroup not found with id: " + groupId));
        if (!group.getCreatedBy().equals(currentUserId)) {
            throw new SecurityException("User does not have permission to modify this group");
        }
        removeMember(groupId, userId);
    }

    @Override
    @Transactional
    public TeacherGroup create(TeacherGroup teacherGroup, String creatorUserId, List<String> userIds) {
        TeacherGroup savedGroup = teacherGroupRepository.save(teacherGroup);

        TeacherGroupRelation creatorRelation = new TeacherGroupRelation();
        creatorRelation.setGroupId(savedGroup.getId());
        creatorRelation.setUserId(creatorUserId);
        teacherGroupRelationRepository.save(creatorRelation);

        if (userIds != null) {
            for (String userId : userIds) {
                if (!userId.equals(creatorUserId)) {
                    TeacherGroupRelation relation = new TeacherGroupRelation();
                    relation.setGroupId(savedGroup.getId());
                    relation.setUserId(userId);
                    teacherGroupRelationRepository.save(relation);
                }
            }
        }

        return savedGroup;
    }

    @Override
    public Optional<TeacherGroup> findById(UUID id) {
        return teacherGroupRepository.findById(id);
    }

    @Override
    public List<TeacherGroup> findAll() {
        return teacherGroupRepository.findAll();
    }

    @Override
    @Transactional
    public TeacherGroup update(UUID id, TeacherGroup updatedGroup) {
        Optional<TeacherGroup> existingGroup = teacherGroupRepository.findById(id);
        if (existingGroup.isPresent()) {
            TeacherGroup group = existingGroup.get();
            group.setName(updatedGroup.getName());
//            group.setStatus(updatedGroup.getStatus());
            group.setDescription(updatedGroup.getDescription());
            return teacherGroupRepository.save(group);
        } else {
            throw new RuntimeException("TeacherGroup not found with id: " + id);
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        List<TeacherGroupRelation> relations = teacherGroupRelationRepository.findAll()
                                                                             .stream()
                                                                             .filter(relation -> relation.getGroupId().equals(id))
                                                                             .collect(Collectors.toList());
        teacherGroupRelationRepository.deleteAll(relations);
        teacherGroupRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<TeacherGroupRelation> addMembers(UUID groupId, List<String> userIds) {
        if (!teacherGroupRepository.existsById(groupId)) {
            throw new RuntimeException("TeacherGroup not found with id: " + groupId);
        }

        List<TeacherGroupRelation> relations = new ArrayList<>();
        for (String userId : userIds) {
            TeacherGroupRelation relation = new TeacherGroupRelation();
            relation.setGroupId(groupId);
            relation.setUserId(userId);
            relations.add(teacherGroupRelationRepository.save(relation));
        }
        return relations;
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
//        dto.setStatus(group.getStatus());
        dto.setDescription(group.getDescription());
        dto.setCreatedBy(group.getCreatedBy());
        dto.setLastModifiedDate(group.getLastModifiedDate());
        return dto;
    }


    private MemberDTO convertToMemberDTO(TeacherGroupRelation relation) {
        MemberDTO dto = new MemberDTO();
        dto.setGroupId(relation.getGroupId());
        dto.setUserId(relation.getUserId());
        String fullName = userService.getUserFullName(relation.getUserId());
        dto.setFullName(fullName);
        dto.setAddedTime(relation.getCreatedDate());
        return dto;
    }
}
