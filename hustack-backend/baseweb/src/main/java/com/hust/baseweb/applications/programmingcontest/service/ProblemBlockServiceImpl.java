package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import com.hust.baseweb.applications.programmingcontest.model.BlockCode;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblem;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemBlockRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemBlockService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProblemBlockServiceImpl implements ProblemBlockService {

    ProblemBlockRepo problemBlockRepo;

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
                                                        .completedBy(Integer.valueOf(1).equals(blockCode.getForStudent()) ? 1 : 0)
                                                        .build();
                problemBlocks.add(problemBlock);
            }
        }

        return problemBlockRepo.saveAll(problemBlocks);
    }
}
