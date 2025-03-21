package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModelInputMapNewProblemToSubmissionsInContest {
    private String contestId;
    private String problemId;
    private String newProblemId;
}
