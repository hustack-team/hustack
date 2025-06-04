package com.hust.baseweb.applications.programmingcontest.model;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AllGroupReponseDTO {
    private UUID id;
    private String name;
//    private String status;
    private String description;
    private String createdBy;
    private LocalDateTime lastModifiedDate;
}
