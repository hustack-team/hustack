package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.model.ModelSearchGroupResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeacherGroupService {
    Page<ModelSearchGroupResult> search(String keyword, List<String> excludeIds, Pageable pageable);
}
