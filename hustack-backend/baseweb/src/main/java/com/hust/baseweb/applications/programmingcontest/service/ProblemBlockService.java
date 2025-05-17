package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import com.hust.baseweb.applications.programmingcontest.model.BlockCode;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblem;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemBlockRepo;
import jakarta.transaction.Transactional;
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

    @Transactional
    public List<ProblemBlock> createProblemBlocks(String problemId, List<BlockCode> blockCodes) {
        if (blockCodes == null || blockCodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProblemBlock> problemBlocks = new ArrayList<>();

        Map<String, List<BlockCode>> blocksByLanguage =
            blockCodes.stream()
                      .collect(Collectors.groupingBy(BlockCode::getLanguage));

        for (List<BlockCode> blocks : blocksByLanguage.values()) {
            for (int i = 0; i < blocks.size(); i++) {
                BlockCode blockCode = blocks.get(i);
                ProblemBlock problemBlock = ProblemBlock.builder()
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
    public List<BlockCode> getBlockCodesByProblemId(String problemId) {
        List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
        return problemBlocks.stream()
                            .map(pb -> {
                                BlockCode blockCode = new BlockCode();
                                blockCode.setId(pb.getId().toString());
                                blockCode.setCode(pb.getSourceCode());
                                blockCode.setForStudent(pb.getCompletedBy() == 1);
                                blockCode.setLanguage(pb.getProgrammingLanguage());
                                return blockCode;
                            })
                            .collect(Collectors.toList());
    }
}
