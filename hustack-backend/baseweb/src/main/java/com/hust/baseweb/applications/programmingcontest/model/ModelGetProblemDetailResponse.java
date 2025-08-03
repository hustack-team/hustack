package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelGetProblemDetailResponse implements Serializable {

    String problemId;

    String problemName;

    String levelId;

    String problemDescription;

    String createdByUserId;

    String submissionMode;

    boolean unauthorized;

    String problemRename;

    String problemRecode;

    String forbiddenInstructions;

    int testCasesCount;

    int totalPointTestCase;

    Double coefficientPoint;
}
