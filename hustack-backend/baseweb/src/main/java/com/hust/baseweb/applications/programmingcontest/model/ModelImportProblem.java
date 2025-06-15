package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class ModelImportProblem {
    @NotBlank
    String problemId;

    @NotBlank
    String problemName;
}

