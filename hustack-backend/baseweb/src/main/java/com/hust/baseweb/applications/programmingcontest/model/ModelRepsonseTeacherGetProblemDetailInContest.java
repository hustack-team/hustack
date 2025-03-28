package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModelRepsonseTeacherGetProblemDetailInContest {
    private String problemId;
    private String problemName;
    private String problemDescription;
    private String solutionCode;
}
