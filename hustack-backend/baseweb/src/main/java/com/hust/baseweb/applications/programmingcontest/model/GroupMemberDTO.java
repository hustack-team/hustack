package com.hust.baseweb.applications.programmingcontest.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class GroupMemberDTO {
    private UUID id;
    private String name;
//    private String status;
    private String description;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
    private List<String> userIds;
}
