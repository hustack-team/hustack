package com.hust.baseweb.applications.programmingcontest.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ModelSearchGroupResult {
    private String id;
    private String name;
    private int memberCount;
    private String status;
    private String description;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
}
