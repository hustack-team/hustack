package com.hust.baseweb.applications.education.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class CreateAssignmentIM {

    @NotNull
    private UUID classId;

    @NotBlank
    private String name;

    @NotNull
    private Date openTime;

    @NotNull
    private Date closeTime;

    private String subject;
}
