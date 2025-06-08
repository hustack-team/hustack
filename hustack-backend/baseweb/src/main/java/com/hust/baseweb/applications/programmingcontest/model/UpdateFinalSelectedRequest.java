package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;
@Getter
@NotNull
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateFinalSelectedRequest {
    @NotNull(message = "New submission ID cannot be null")
    UUID newSubmissionId;
    UUID oldSubmissionId;
}
