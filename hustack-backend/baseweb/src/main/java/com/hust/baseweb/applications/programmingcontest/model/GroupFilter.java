package com.hust.baseweb.applications.programmingcontest.model;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class GroupFilter {
    private int page = 0;
    private int size = 5;
    private String keyword;
    private String status;
    private List<String> excludeIds;

    public void normalize() {
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }
        if (status != null && status.isEmpty()) {
            status = null;
        }
        if (excludeIds != null && excludeIds.isEmpty()) {
            excludeIds = null;
        }
    }
}
