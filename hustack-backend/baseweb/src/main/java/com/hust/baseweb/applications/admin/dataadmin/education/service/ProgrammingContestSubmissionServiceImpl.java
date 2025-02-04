package com.hust.baseweb.applications.admin.dataadmin.education.service;

import com.hust.baseweb.applications.admin.dataadmin.education.model.ProgrammingContestSubmissionOM;
import com.hust.baseweb.applications.programmingcontest.repo.ContestSubmissionRepo;
import com.hust.baseweb.model.SubmissionFilter;
import com.hust.baseweb.model.SubmissionProjection;
import com.hust.baseweb.service.UserService;
import com.hust.baseweb.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProgrammingContestSubmissionServiceImpl {

    private final ContestSubmissionRepo contestSubmissionRepo;

    private final UserService userService;

    public Page<ProgrammingContestSubmissionOM> findContestSubmissionsOfStudent(
        String studentLoginId,
        String search,
        Pageable pageable
    ) {
        return contestSubmissionRepo.findContestSubmissionsOfStudent(studentLoginId, search, pageable);
    }

    public Page<SubmissionProjection> search(SubmissionFilter filter) {
        String userId = StringUtils.isNotBlank(filter.getUserId()) ? filter.getUserId().trim() : null;
        String contestId = StringUtils.isNotBlank(filter.getContestId()) ? filter.getContestId().trim() : null;
        String problemId = StringUtils.isNotBlank(filter.getProblemId()) ? filter.getProblemId().trim() : null;
        normalizeFilter(filter);
        Pageable pageable = CommonUtils.getPageable(
            filter.getPage(),
            filter.getSize(),
            Sort.by("created_stamp").descending());

        return contestSubmissionRepo.findAllBy(
            userId,
            contestId,
            problemId,
            filter.getLanguages(),
            filter.getStatuses(),
            filter.getFromDate(),
            filter.getToDate(),
            pageable);
    }

    private void normalizeFilter(SubmissionFilter filter) {
        if (StringUtils.isBlank(filter.getLanguages())) {
            filter.setLanguages(null);
        }
        if (StringUtils.isBlank(filter.getStatuses())) {
            filter.setStatuses(null);
        }
    }
}
