package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GrantProblemPermissionDTO {

    List<String> userIds;

    List<UUID> groupIds;

    @NotBlank(message = "Problem ID is required")
    String problemId;

    @NotBlank(message = "Role ID is required")
    String roleId;
}
