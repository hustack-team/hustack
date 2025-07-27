package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepo extends JpaRepository<TagEntity, Integer> {

    TagEntity findByTagId(Integer tagId);
    
    @Query("SELECT t FROM TagEntity t WHERE t.tagId IN :tagIds")
    List<TagEntity> findAllByTagIdIn(@Param("tagIds") List<Integer> tagIds);
}
