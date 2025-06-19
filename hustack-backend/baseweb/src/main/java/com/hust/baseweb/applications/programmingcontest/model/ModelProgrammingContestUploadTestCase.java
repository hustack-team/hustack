package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModelProgrammingContestUploadTestCase {

    @NotBlank
    private String problemId;

    @NotNull
    private Boolean isPublic;

    @Min(1)
    private int point;

    private String correctAnswer;

    private String description;

    @NotNull
    private TestcaseUploadMode uploadMode;
}
