package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GroupDTO {
    private UUID id;
    @NotBlank(message = "Group name is required")
    private String name;
    private String status;
    private String description;
    private String createdByUserId;
    private List<String> userIds;
}
