package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TeacherGroupService {
    private final TeacherGroupRepository teacherGroupRepository;
    private final TeacherGroupRelationRepository teacherGroupRelationRepository;

    @Autowired
    public TeacherGroupService(
        TeacherGroupRepository teacherGroupRepository,
        TeacherGroupRelationRepository teacherGroupRelationRepository
    ) {
        this.teacherGroupRepository = teacherGroupRepository;
        this.teacherGroupRelationRepository = teacherGroupRelationRepository;
    }

    @Transactional
    public TeacherGroup create(TeacherGroup teacherGroup, String creatorUserId, List<String> userIds) {
        TeacherGroup savedGroup = teacherGroupRepository.save(teacherGroup);

        // Add creator as a member
        TeacherGroupRelation creatorRelation = new TeacherGroupRelation();
        creatorRelation.setGroupId(savedGroup.getId());
        creatorRelation.setUserId(creatorUserId);
        teacherGroupRelationRepository.save(creatorRelation);

        // Add other users as members
        if (userIds != null) {
            for (String userId : userIds) {
                if (!userId.equals(creatorUserId)) { // Avoid adding creator twice
                    TeacherGroupRelation relation = new TeacherGroupRelation();
                    relation.setGroupId(savedGroup.getId());
                    relation.setUserId(userId);
                    teacherGroupRelationRepository.save(relation);
                }
            }
        }

        return savedGroup;
    }

    public Optional<TeacherGroup> findById(UUID id) {
        return teacherGroupRepository.findById(id);
    }

    public List<TeacherGroup> findAll() {
        return teacherGroupRepository.findAll();
    }

    @Transactional
    public TeacherGroup update(UUID id, TeacherGroup updatedGroup) {
        Optional<TeacherGroup> existingGroup = teacherGroupRepository.findById(id);
        if (existingGroup.isPresent()) {
            TeacherGroup group = existingGroup.get();
            group.setName(updatedGroup.getName());
            group.setStatus(updatedGroup.getStatus());
            group.setDescription(updatedGroup.getDescription());
            return teacherGroupRepository.save(group);
        } else {
            throw new RuntimeException("TeacherGroup not found with id: " + id);
        }
    }

    @Transactional
    public void deleteById(UUID id) {
        List<TeacherGroupRelation> relations = teacherGroupRelationRepository.findAll()
                                                                             .stream()
                                                                             .filter(relation -> relation.getGroupId().equals(id))
                                                                             .toList();
        teacherGroupRelationRepository.deleteAll(relations);
        teacherGroupRepository.deleteById(id);
    }

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

    @Transactional
    public void removeMember(UUID groupId, String userId) {
        TeacherGroupRelationId id = new TeacherGroupRelationId();
        id.setGroupId(groupId);
        id.setUserId(userId);
        teacherGroupRelationRepository.deleteById(id);
    }
}
