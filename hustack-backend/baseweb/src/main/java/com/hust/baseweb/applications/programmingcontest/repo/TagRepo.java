package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepo extends JpaRepository<TagEntity, Integer> {

    TagEntity findByTagId(Integer tagId);
    TagEntity findByName(String name);
    List<TagEntity> findByNameInIgnoreCase(List<String> names);
}
