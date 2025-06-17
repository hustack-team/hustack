package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupMemberDTO {
    UUID id;
    String name;
    String description;
    String createdBy;
    LocalDateTime lastModifiedDate;
    List<String> userIds;
}
