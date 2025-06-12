package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllGroupReponseDTO {
    UUID id;
    String name;
    String description;
    String createdBy;
    LocalDateTime lastModifiedDate;
}
