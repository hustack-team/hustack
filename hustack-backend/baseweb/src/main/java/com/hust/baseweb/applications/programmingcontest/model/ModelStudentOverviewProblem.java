package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelStudentOverviewProblem {

    String problemId;

    String problemName;

    String problemCode;

    String levelId;

    boolean submitted = false;

    boolean accepted = false;

    Long maxSubmittedPoint;

    Long maxPoint;

    List<String> tags = new ArrayList<>();

    int blockProblem;

    List<BlockCode> blockCodes;
}
