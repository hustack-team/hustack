package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.model.ModelSearchGroupResult;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeacherGroupServiceImpl implements TeacherGroupService {

    private final TeacherGroupRepository teacherGroupRepository;
    private final TeacherGroupRelationRepository teacherGroupRelationRepository;

    @Autowired
    public TeacherGroupServiceImpl(
        TeacherGroupRepository teacherGroupRepository,
        TeacherGroupRelationRepository teacherGroupRelationRepository
    ) {
        this.teacherGroupRepository = teacherGroupRepository;
        this.teacherGroupRelationRepository = teacherGroupRelationRepository;
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
            group.setStatus(updatedGroup.getStatus());
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

    @Override
    public Page<ModelSearchGroupResult> search(String keyword, List<String> excludeIds, Pageable pageable, String userId) {
        List<TeacherGroup> allGroups = teacherGroupRepository.findAll();

        List<UUID> groupIdsByUser = teacherGroupRelationRepository.findGroupIdsByUserIdContaining(keyword);

        List<ModelSearchGroupResult> filtered = allGroups.stream()
                                                         .filter(g -> g.getCreatedByUserId().equals(userId)) // Chỉ lấy nhóm do userId tạo
                                                         .filter(g -> !excludeIds.contains(g.getId().toString()))
                                                         .filter(g ->
                                                                     g.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                                                                     groupIdsByUser.contains(g.getId())
                                                         )
                                                         .map(g -> {
                                                             ModelSearchGroupResult result = new ModelSearchGroupResult();
                                                             result.setId(g.getId().toString());
                                                             result.setName(g.getName());
                                                             return result;
                                                         })
                                                         .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<ModelSearchGroupResult> paginated = filtered.subList(start, end);
        return new PageImpl<>(paginated, pageable, filtered.size());
    }
}
