package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelUserProblemRoleInput {
    List<String> userIds;
    List<UUID> groupIds;
    String problemId;
    String roleId;
}
