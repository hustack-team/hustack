package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;

import java.util.List;
import java.util.Optional;

public interface TeacherGroupRelationServiceRepo {
    TeacherGroupRelation save(TeacherGroupRelation teacherGroupRelation);
    Optional<TeacherGroupRelation> findById(TeacherGroupRelationId id);
    List<TeacherGroupRelation> findAll();
    void deleteById(TeacherGroupRelationId id);
}
