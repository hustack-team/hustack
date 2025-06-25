package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class ModelImportProblem {

    @NotBlank(message = "Problem ID is required")
    String problemId;

    @NotBlank(message = "Problem name is required")
    String problemName;

    @NotNull(message = "C++ time limit is required")
    Float timeLimitCPP;

    @NotNull(message = "Java time limit is required")
    Float timeLimitJAVA;

    @NotNull(message = "Python time limit is required")
    Float timeLimitPYTHON;

    @NotNull(message = "Memory limit is required")
    Float memoryLimit;

    @NotNull(message = "Level order is required")
    Integer levelOrder;

    @NotNull(message = "Appearances is required")
    Integer appearances;

    @NotNull(message = "Public problem status is required")
    Boolean isPublicProblem;
    Integer categoryId;
    String levelId;
    String statusId;
    String problemDescription;
    String correctSolutionLanguage;
    String correctSolutionSourceCode;
    String scoreEvaluationType;
    String solution;
    Boolean isPreloadCode;
    String preloadCode;
    String sampleTestCase;
    String solutionCheckerSourceLanguage;
    String solutionCheckerSourceCode;
    List<String> tags;
    List<Map<String, Object>> testCases;
    String[] fileId;
    List<BlockCode> blockCodes;
}
