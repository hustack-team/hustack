package com.hust.baseweb.applications.programmingcontest.controller;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.model.GroupDTO;
import com.hust.baseweb.applications.programmingcontest.model.MemberDTO;
import com.hust.baseweb.applications.programmingcontest.service.TeacherGroupService;
import com.hust.baseweb.applications.programmingcontest.service.TeacherGroupRelationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final TeacherGroupService teacherGroupService;
    private final TeacherGroupRelationService teacherGroupRelationService;

    @Autowired
    public MemberController(
        TeacherGroupService teacherGroupService,
        TeacherGroupRelationService teacherGroupRelationService
    ) {
        this.teacherGroupService = teacherGroupService;
        this.teacherGroupRelationService = teacherGroupRelationService;
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupDTO> createGroup(@Valid @RequestBody GroupDTO groupDTO, Authentication authentication) {
        try {
            String userId = authentication.getName(); // Lấy userId từ token
            TeacherGroup group = new TeacherGroup();
            group.setName(groupDTO.getName());
            group.setStatus(groupDTO.getStatus());
            group.setDescription(groupDTO.getDescription());
            group.setCreatedByUserId(userId);

            // Gọi service để tạo nhóm và thêm thành viên
            TeacherGroup savedGroup = teacherGroupService.create(group, userId, (List<String>) groupDTO.getUserIds());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToGroupDTO(savedGroup));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupDTO> getGroup(@PathVariable UUID id) {
        return teacherGroupService.findById(id)
                                  .map(group -> ResponseEntity.ok(convertToGroupDTO(group)))
                                  .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupDTO>> getAllGroups() {
        List<GroupDTO> groups = teacherGroupService.findAll()
                                                   .stream()
                                                   .map(this::convertToGroupDTO)
                                                   .collect(Collectors.toList());
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<GroupDTO> updateGroup(@PathVariable UUID id, @Valid @RequestBody GroupDTO groupDTO) {
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
    public ResponseEntity<MemberDTO> addMember(
        @PathVariable UUID groupId,
        @Valid @RequestBody MemberDTO memberDTO
    ) {
        try {
            TeacherGroupRelation relation = teacherGroupService.addMember(
                groupId,
                memberDTO.getUserId(),
                memberDTO.getRole()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToMemberDTO(relation));
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

    @PutMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<MemberDTO> updateMember(
        @PathVariable UUID groupId,
        @PathVariable String userId,
        @Valid @RequestBody MemberDTO memberDTO
    ) {
        try {
            TeacherGroupRelationId id = new TeacherGroupRelationId();
            id.setGroupId(groupId);
            id.setUserId(userId);

            return teacherGroupRelationService.findById(id)
                                              .map(relation -> {
                                                  relation.setRole(memberDTO.getRole());
                                                  TeacherGroupRelation updatedRelation = teacherGroupRelationService.save(relation);
                                                  return ResponseEntity.ok(convertToMemberDTO(updatedRelation));
                                              })
                                              .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
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

    private GroupDTO convertToGroupDTO(TeacherGroup group) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setStatus(group.getStatus());
        dto.setDescription(group.getDescription());
        dto.setCreatedByUserId(group.getCreatedByUserId());
        return dto;
    }

    private MemberDTO convertToMemberDTO(TeacherGroupRelation relation) {
        MemberDTO dto = new MemberDTO();
        dto.setGroupId(relation.getGroupId());
        dto.setUserId(relation.getUserId());
        dto.setRole(relation.getRole());
        return dto;
    }
}
