package com.hust.baseweb.applications.programmingcontest.controller;

import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class GroupMemberController {

    TeacherGroupService teacherGroupService;

    @Secured("ROLE_TEACHER")
    @PostMapping("/groups")
    public ResponseEntity<GroupMemberDTO> createGroup(@Valid @RequestBody GroupMemberDTO groupDTO, Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupMemberDTO createdGroup = teacherGroupService.createGroup(groupDTO, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/groups/{id}")
    public ResponseEntity<AllGroupReponseDTO> getGroup(@PathVariable UUID id, Authentication authentication) {
        try {
            String userId = authentication.getName();
            AllGroupReponseDTO group = teacherGroupService.getGroup(id, userId);
            return ResponseEntity.ok(group);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/groups")
    public ResponseEntity<?> getAllMyGroups(Authentication authentication, GroupFilter filter) {
        try {
            String userId = authentication.getName();
            Page<ModelSearchGroupResult> groups = teacherGroupService.getGroups(filter, userId);
            return ResponseEntity.ok().body(groups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @Secured("ROLE_TEACHER")
    @PutMapping("/groups/{id}")
    public ResponseEntity<GroupMemberDTO> updateGroup(@PathVariable UUID id, @Valid @RequestBody GroupMemberDTO groupDTO, Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupMemberDTO updatedGroup = teacherGroupService.updateGroup(id, groupDTO, userId);
            return ResponseEntity.ok(updatedGroup);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id, Authentication authentication) {
        try {
            String userId = authentication.getName();
            teacherGroupService.deleteGroup(id, userId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Secured("ROLE_TEACHER")
    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<List<MemberDTO>> addMembers(
        @PathVariable UUID groupId,
        @Valid @RequestBody List<String> userIds,
        Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            List<MemberDTO> members = teacherGroupService.addGroupMembers(groupId, userIds, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(members);
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable UUID groupId, Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<MemberDTO> members = teacherGroupService.getGroupMembers(groupId, userId);
            return ResponseEntity.ok(members);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @Secured("ROLE_TEACHER")
    @GetMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<MemberDTO> getMember(@PathVariable UUID groupId, @PathVariable String userId, Authentication authentication) {
        try {
            String currentUserId = authentication.getName();
            MemberDTO member = teacherGroupService.getGroupMember(groupId, userId, currentUserId);
            return ResponseEntity.ok(member);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Secured("ROLE_TEACHER")
    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID groupId, @PathVariable String userId, Authentication authentication) {
        try {
            String currentUserId = authentication.getName();
            teacherGroupService.removeGroupMember(groupId, userId, currentUserId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
