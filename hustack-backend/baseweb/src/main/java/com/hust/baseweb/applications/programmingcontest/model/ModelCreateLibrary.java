package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

@Data
public class ModelCreateLibrary {

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @NotBlank(message = "Language cannot be blank")
    @Size(max = 200, message = "Language must be less than 200 characters")
    private String language;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @Size(max = 100, message = "Status must be less than 100 characters")
    private String status;

    @NotNull(message = "Created Stamp cannot be null")
    private Date createdStamp;

    @NotNull(message = "Last Updated Stamp cannot be null")
    private Date lastUpdatedStamp;
}
