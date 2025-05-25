package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.model.ModelSearchGroupResult;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeacherGroupServiceImpl implements TeacherGroupService {

    @Autowired
    private TeacherGroupRepository teacherGroupRepository;

    @Autowired
    private TeacherGroupRelationRepository teacherGroupRelationRepository;

    @Override
    public Page<ModelSearchGroupResult> search(String keyword, List<String> excludeIds, Pageable pageable) {
        List<TeacherGroup> allGroups = teacherGroupRepository.findAll();

        List<UUID> groupIdsByUser = teacherGroupRelationRepository.findGroupIdsByUserIdContaining(keyword);

        List<ModelSearchGroupResult> filtered = allGroups.stream()
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
