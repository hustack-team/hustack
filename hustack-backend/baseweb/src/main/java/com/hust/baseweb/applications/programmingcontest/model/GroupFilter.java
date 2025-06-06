package com.hust.baseweb.applications.programmingcontest.model;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Optional;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupFilter {
    int page = 0;
    int size = 5;
    String keyword;
    String status;

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
    }
}
