package com.hust.baseweb.applications.programmingcontest.model;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProblemDTO {

    String problemId;

    String problemName;

    String problemDescription;

    float timeLimit;

    float timeLimitCPP;

    float timeLimitJAVA;

    float timeLimitPYTHON;

    float memoryLimit;

    String levelId;

    Integer categoryId;

    String correctSolutionSourceCode;

    String correctSolutionLanguage;

    String solutionChecker;

    String solutionCheckerLanguage;

//    Boolean isPreloadCode; // Preload Code functionality - DISABLED

//    String preloadCode; // Preload Code functionality - DISABLED

    Boolean isPublic;

    String scoreEvaluationType;

    String[] fileId;

    Integer[] tagIds;

    ProblemStatus status;

    String sampleTestCase;

    private List<BlockCode> blockCodes;

}
