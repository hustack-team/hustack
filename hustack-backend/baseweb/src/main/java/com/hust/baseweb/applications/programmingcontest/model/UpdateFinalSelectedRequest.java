package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;
@Getter
@NotNull
public class UpdateFinalSelectedRequest {


    @NotNull(message = "New submission ID cannot be null")
    private UUID newSubmissionId;
    private UUID oldSubmissionId;
}
