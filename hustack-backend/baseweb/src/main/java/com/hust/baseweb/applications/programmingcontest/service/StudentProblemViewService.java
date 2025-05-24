package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentProblemViewService {

    private final ContestRepo contestRepo;
    private final ContestProblemRepo contestProblemRepo;
    private final ProblemRepo problemRepo;
    private final ProblemBlockRepo problemBlockRepo;
    private final ProblemTestCaseService problemTestCaseService;
    private final ContestService contestService;
    private final ContestSubmissionRepo contestSubmissionRepo;

    public ModelStudentViewProblemDetail getProblemDetailForStudentView(String userId, String contestId, String problemId) {
        try {
            ContestEntity contestEntity = contestRepo.findContestByContestId(contestId);
            if (contestEntity == null) return null;

            ContestProblem cp = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
            if (cp == null) return null;

            if (!ContestEntity.CONTEST_STATUS_RUNNING.equals(contestEntity.getStatusId())) {
                return null;
            }

            ModelCreateContestProblemResponse problemEntity = problemTestCaseService.getContestProblem(problemId);
            ProblemEntity problem = problemRepo.findByProblemId(problemId);
            if (problem == null) return null;

            ModelStudentViewProblemDetail model = new ModelStudentViewProblemDetail();

            if (ContestEntity.CONTEST_PROBLEM_DESCRIPTION_VIEW_TYPE_HIDDEN.equals(contestEntity.getProblemDescriptionViewType())) {
                model.setProblemStatement(" ");
            } else {
                model.setProblemStatement(problemEntity.getProblemDescription());
            }

            model.setSubmissionMode(cp.getSubmissionMode());
            model.setProblemName(cp.getProblemRename());
            model.setProblemCode(cp.getProblemRecode());
            model.setIsPreloadCode(problemEntity.getIsPreloadCode());
            model.setPreloadCode(problemEntity.getPreloadCode());
            model.setAttachment(problemEntity.getAttachment());
            model.setAttachmentNames(problemEntity.getAttachmentNames());
            model.setListLanguagesAllowed(contestEntity.getListLanguagesAllowedInContest());
            model.setSampleTestCase(problemEntity.getSampleTestCase());

            if (problem.getCategoryId() != null && Integer.valueOf(1).equals(problem.getCategoryId())) {
                List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
                List<BlockCode> blockCodes = problemBlocks.stream()
                                                                                    .map(pb -> {
                                                                                        BlockCode blockCode = new BlockCode();
                                                                                        blockCode.setId(pb.getId().toString());
                                                                                        blockCode.setCode(Integer.valueOf(1).equals(pb.getCompletedBy())? "" : pb.getSourceCode());
                                                                                        blockCode.setForStudent(pb.getCompletedBy());
                                                                                        blockCode.setLanguage(pb.getProgrammingLanguage());
                                                                                        blockCode.setSeq(pb.getSeq());
                                                                                        return blockCode;
                                                                                    })
                                                                                    .collect(Collectors.toList());
                model.setBlockCodes(blockCodes);
            }

            return model;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<ModelStudentOverviewProblem> getStudentOverviewProblems(String userId, String contestId) {
        ContestEntity contest = contestService.findContest(contestId);

        List<ProblemEntity> problems = contest.getProblems();
        List<String> acceptedProblems = contestSubmissionRepo.findAcceptedProblemsOfUser(userId, contestId);
        List<ModelProblemMaxSubmissionPoint> submittedProblems = contestSubmissionRepo.findSubmittedProblemsOfUser(userId, contestId);

        Map<String, Long> mapProblemToMaxSubmissionPoint = submittedProblems.stream()
                                                                            .collect(Collectors.toMap(ModelProblemMaxSubmissionPoint::getProblemId, ModelProblemMaxSubmissionPoint::getMaxPoint));

        List<ModelStudentOverviewProblem> responses = new ArrayList<>();

        if (!ContestEntity.CONTEST_STATUS_RUNNING.equals(contest.getStatusId())) {
            return responses;
        }

        Set<String> problemIds = problems.stream().map(ProblemEntity::getProblemId).collect(Collectors.toSet());
        List<ContestProblem> contestProblems = contestProblemRepo.findByContestIdAndProblemIdIn(contestId, problemIds);
        Map<String, ContestProblem> problemId2ContestProblem = contestProblems.stream()
                                                                              .collect(Collectors.toMap(ContestProblem::getProblemId, cp -> cp));

        for (ProblemEntity problem : problems) {
            String problemId = problem.getProblemId();
            ContestProblem contestProblem = problemId2ContestProblem.get(problemId);
            if (contestProblem == null) continue;

            if (ContestProblem.SUBMISSION_MODE_HIDDEN.equals(contestProblem.getSubmissionMode())) continue;

            ModelStudentOverviewProblem response = new ModelStudentOverviewProblem();
            response.setProblemId(problemId);
            response.setProblemName(contestProblem.getProblemRename());
            response.setProblemCode(contestProblem.getProblemRecode());
            response.setLevelId(problem.getLevelId());

            Integer blockProblem = problem.getCategoryId();
            int blockProblemValue = blockProblem != null ? blockProblem : 0;
            response.setBlockProblem(blockProblemValue);

            if (Integer.valueOf(1).equals(blockProblemValue)) {
                List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
                List<BlockCode> blockCodes = problemBlocks.stream().map(pb -> {
                    BlockCode blockCode = new BlockCode();
                    blockCode.setId(pb.getId().toString());
                    blockCode.setCode(pb.getSourceCode());
                    blockCode.setForStudent(pb.getCompletedBy());
                    blockCode.setLanguage(pb.getProgrammingLanguage());
                    return blockCode;
                }).collect(Collectors.toList());
                response.setBlockCodes(blockCodes);
            }

            if ("N".equals(contest.getContestShowTag())) {
                response.setTags(new ArrayList<>());
            } else {
                List<String> tags = problem.getTags().stream().map(TagEntity::getName).collect(Collectors.toList());
                response.setTags(tags);
            }

            if (mapProblemToMaxSubmissionPoint.containsKey(problemId)) {
                response.setSubmitted(true);
                response.setMaxSubmittedPoint(mapProblemToMaxSubmissionPoint.get(problemId));
            }

            if (acceptedProblems.contains(problemId)) {
                response.setAccepted(true);
            }

            responses.add(response);
        }

        return responses;
    }

}
