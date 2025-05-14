package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblem;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemBlockRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProblemBlockService {

    private final ProblemBlockRepo problemBlockRepo;

    public List<ProblemBlock> createProblemBlocks(String problemId, List<ModelCreateContestProblem.BlockCode> blockCodes) {
        if (blockCodes == null || blockCodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProblemBlock> problemBlocks = new ArrayList<>();

        Map<String, List<ModelCreateContestProblem.BlockCode>> blocksByLanguage =
            blockCodes.stream()
                      .collect(Collectors.groupingBy(ModelCreateContestProblem.BlockCode::getLanguage));

        for (List<ModelCreateContestProblem.BlockCode> blocks : blocksByLanguage.values()) {
            for (int i = 0; i < blocks.size(); i++) {
                ModelCreateContestProblem.BlockCode blockCode = blocks.get(i);
                ProblemBlock problemBlock = ProblemBlock.builder()
                                                        .id(UUID.randomUUID())
                                                        .problemId(problemId)
                                                        .seq(i + 1)
                                                        .sourceCode(blockCode.getCode())
                                                        .programmingLanguage(blockCode.getLanguage())
                                                        .completedBy(blockCode.isForStudent() ? 1 : 0)
                                                        .build();
                problemBlocks.add(problemBlock);
            }
        }

        return problemBlockRepo.saveAll(problemBlocks);
    }
    public List<ModelCreateContestProblem.BlockCode> getBlockCodesByProblemId(String problemId) {
        List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
        return problemBlocks.stream()
                            .map(pb -> {
                                ModelCreateContestProblem.BlockCode blockCode = new ModelCreateContestProblem.BlockCode();
                                blockCode.setId(pb.getId().toString());
                                blockCode.setCode(pb.getSourceCode());
                                blockCode.setForStudent(pb.getCompletedBy() == 1);
                                blockCode.setLanguage(pb.getProgrammingLanguage());
                                return blockCode;
                            })
                            .collect(Collectors.toList());
    }
}
