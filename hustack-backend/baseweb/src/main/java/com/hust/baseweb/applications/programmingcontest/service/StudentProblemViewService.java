package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.ContestEntity;
import com.hust.baseweb.applications.programmingcontest.entity.ContestProblem;
import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblem;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblemResponse;
import com.hust.baseweb.applications.programmingcontest.model.ModelStudentViewProblemDetail;
import com.hust.baseweb.applications.programmingcontest.repo.ContestProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ContestRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemBlockRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentProblemViewService {

    private final ContestRepo contestRepo;
    private final ContestProblemRepo contestProblemRepo;
    private final ProblemRepo problemRepo;
    private final ProblemBlockRepo problemBlockRepo;
    private final ProblemTestCaseService problemTestCaseService;

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

            if (problem.getBlockProblem() != null && problem.getBlockProblem() == 1) {
                List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
                List<ModelCreateContestProblem.BlockCode> blockCodes = problemBlocks.stream()
                                                                                    .map(pb -> {
                                                                                        ModelCreateContestProblem.BlockCode blockCode = new ModelCreateContestProblem.BlockCode();
                                                                                        blockCode.setId(pb.getId().toString());
                                                                                        blockCode.setCode(pb.getCompletedBy() == 1 ? "" : pb.getSourceCode());
                                                                                        blockCode.setForStudent(pb.getCompletedBy() == 1);
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
}
