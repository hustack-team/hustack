package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelSearchGroupResult {
    String id;
    String name;
    int memberCount;
    String status;
    String description;
    String createdBy;
    LocalDateTime lastModifiedDate;
}
