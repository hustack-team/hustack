package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemRepo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemCacheService {

    ProblemRepo problemRepo;

    static final String HASH = "PROBLEM";

    @CachePut(value = HASH, key = "#result.problemId")
    public ProblemEntity saveProblemWithCache(ProblemEntity problem) {
        return saveProblem(problem);
    }

    public ProblemEntity saveProblem(ProblemEntity problem) {
        return problemRepo.save(problem);
    }
}
