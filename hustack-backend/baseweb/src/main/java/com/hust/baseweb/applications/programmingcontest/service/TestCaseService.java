package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TestCaseEntity;
import com.hust.baseweb.applications.programmingcontest.repo.TestCaseRepo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestCaseService {

    TestCaseRepo testCaseRepo;

    static final String HASH = "TEST_CASE";

    @Caching(evict = {
        @CacheEvict(value = HASH, key = "#testCase.problemId + '_true'"),
        @CacheEvict(value = HASH, key = "#testCase.problemId + '_false'")
    })
    public TestCaseEntity saveTestCaseWithCache(TestCaseEntity testCase) {
        return saveTestCase(testCase);
    }

    public TestCaseEntity saveTestCase(TestCaseEntity testCase) {
        return testCaseRepo.save(testCase);
    }

}
