package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import com.hust.baseweb.applications.programmingcontest.model.BlockCode;

import java.util.List;

public interface ProblemBlockService {
    List<ProblemBlock> createProblemBlocks(String problemId, List<BlockCode> blockCodes);
}
