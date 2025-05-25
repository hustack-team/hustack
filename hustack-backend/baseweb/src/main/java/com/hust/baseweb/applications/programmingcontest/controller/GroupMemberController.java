package com.hust.baseweb.applications.programmingcontest.controller;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.model.GroupMemberDTO;
import com.hust.baseweb.applications.programmingcontest.model.MemberDTO;
import com.hust.baseweb.applications.programmingcontest.model.ModelSearchGroupResult;
import com.hust.baseweb.applications.programmingcontest.service.TeacherGroupService;
import com.hust.baseweb.applications.programmingcontest.service.TeacherGroupRelationService;
import com.hust.baseweb.applications.programmingcontest.service.TeacherGroupServiceImpl;
import com.hust.baseweb.service.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/members")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupMemberController {

    TeacherGroupService teacherGroupService;
    TeacherGroupRelationService teacherGroupRelationService;
    private final UserServiceImpl userServiceImpl;
    private final TeacherGroupServiceImpl teacherGroupServiceImpl;

    @Autowired
    public GroupMemberController(
        TeacherGroupService teacherGroupService,
        TeacherGroupRelationService teacherGroupRelationService,
        UserServiceImpl userServiceImpl,
        TeacherGroupServiceImpl teacherGroupServiceImpl
    ) {
        this.teacherGroupService = teacherGroupService;
        this.teacherGroupRelationService = teacherGroupRelationService;
        this.userServiceImpl = userServiceImpl;
        this.teacherGroupServiceImpl = teacherGroupServiceImpl;
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupMemberDTO> createGroup(@Valid @RequestBody GroupMemberDTO groupDTO, Authentication authentication) {
        try {
            String userId = authentication.getName();
            TeacherGroup group = new TeacherGroup();
            group.setName(groupDTO.getName());
            group.setStatus(groupDTO.getStatus());
            group.setDescription(groupDTO.getDescription());
            group.setCreatedByUserId(userId);

            TeacherGroup savedGroup = teacherGroupService.create(group, userId, (List<String>) groupDTO.getUserIds());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToGroupDTO(savedGroup));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupMemberDTO> getGroup(@PathVariable UUID id) {
        return teacherGroupService.findById(id)
                                  .map(group -> ResponseEntity.ok(convertToGroupDTO(group)))
                                  .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/search-groups")
    public ResponseEntity<?> search(
        Pageable pageable,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "exclude", required = false) List<String> excludeIds
    ) {
        if (keyword == null) {
            keyword = "";
        }

        if (excludeIds == null) {
            excludeIds = Collections.emptyList();
        }

        Page<ModelSearchGroupResult> resp = teacherGroupServiceImpl.search(keyword, excludeIds, pageable);
        return ResponseEntity.status(200).body(resp);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupMemberDTO>> getAllGroups() {
        List<GroupMemberDTO> groups = teacherGroupService.findAll()
                                                   .stream()
                                                   .map(this::convertToGroupDTO)
                                                   .collect(Collectors.toList());
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<GroupMemberDTO> updateGroup(@PathVariable UUID id, @Valid @RequestBody GroupMemberDTO groupDTO) {
        try {
            TeacherGroup group = new TeacherGroup();
            group.setName(groupDTO.getName());
            group.setStatus(groupDTO.getStatus());
            group.setDescription(groupDTO.getDescription());

            TeacherGroup updatedGroup = teacherGroupService.update(id, group);
            return ResponseEntity.ok(convertToGroupDTO(updatedGroup));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        try {
            teacherGroupService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<List<MemberDTO>> addMembers(
        @PathVariable UUID groupId,
        @Valid @RequestBody List<String> userIds
    ) {
        try {
            List<TeacherGroupRelation> relations = teacherGroupService.addMembers(groupId, userIds);
            List<MemberDTO> memberDTOs = relations.stream()
                                                  .map(this::convertToMemberDTO)
                                                  .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.CREATED).body(memberDTOs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable UUID groupId) {
        List<MemberDTO> members = teacherGroupRelationService.findAll()
                                                             .stream()
                                                             .filter(relation -> relation.getGroupId().equals(groupId))
                                                             .map(this::convertToMemberDTO)
                                                             .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    @GetMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<MemberDTO> getMember(@PathVariable UUID groupId, @PathVariable String userId) {
        TeacherGroupRelationId id = new TeacherGroupRelationId();
        id.setGroupId(groupId);
        id.setUserId(userId);
        return teacherGroupRelationService.findById(id)
                                          .map(relation -> ResponseEntity.ok(convertToMemberDTO(relation)))
                                          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID groupId, @PathVariable String userId) {
        try {
            teacherGroupService.removeMember(groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private GroupMemberDTO convertToGroupDTO(TeacherGroup group) {
        GroupMemberDTO dto = new GroupMemberDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setStatus(group.getStatus());
        dto.setDescription(group.getDescription());
        dto.setCreatedByUserId(group.getCreatedByUserId());
        dto.setLastUpdatedStamp(group.getLastUpdatedStamp());
        return dto;
    }

    private MemberDTO convertToMemberDTO(TeacherGroupRelation relation) {
        MemberDTO dto = new MemberDTO();
        dto.setGroupId(relation.getGroupId());
        dto.setUserId(relation.getUserId());
        String fullName = userServiceImpl.getUserFullName(relation.getUserId());
        dto.setFullName(fullName);
        dto.setAddedTime(relation.getCreatedStamp());
        return dto;
    }
}
