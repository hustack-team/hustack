package com.hust.baseweb.applications.programmingcontest.model;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelCreateContestProblem {

    String problemId;

    String problemName;

    String problemDescription;

    float timeLimit;

    float timeLimitCPP;

    float timeLimitJAVA;

    float timeLimitPYTHON;

    float memoryLimit;

    String levelId;

    String categoryId;

    String correctSolutionSourceCode;

    String correctSolutionLanguage;

    String solutionChecker;

    String solutionCheckerLanguage;

    String solution;

    Boolean isPreloadCode;

    String preloadCode;

    Boolean isPublic;

    String scoreEvaluationType;

    String[] fileId;

    Integer[] tagIds;

    ProblemStatus status;

    String sampleTestCase;

    private Integer problemBlock;
    private List<BlockCode> blockCodes;

    @Getter
    @Setter
    public static class BlockCode {
        private String id;
        private String code;
        private boolean forStudent;
        private int seq;
        private String language;
    }
}
