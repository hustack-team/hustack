package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.model.ModelStudentOverviewProblem;
import com.hust.baseweb.applications.programmingcontest.model.ModelStudentViewProblemDetail;

import java.util.List;

public interface StudentProblemViewService {
    ModelStudentViewProblemDetail getProblemDetailForStudentView(String userId, String contestId, String problemId);
    List<ModelStudentOverviewProblem> getStudentOverviewProblems(String userId, String contestId);
}
