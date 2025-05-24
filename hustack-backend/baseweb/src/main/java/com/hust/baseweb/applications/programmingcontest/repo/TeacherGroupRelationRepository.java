package com.hust.baseweb.applications.programmingcontest.repo;


import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherGroupRelationRepository extends JpaRepository<TeacherGroupRelation, TeacherGroupRelationId> {

}
