package com.hust.baseweb.applications.programmingcontest.repo;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeacherGroupRepository extends JpaRepository<TeacherGroup, UUID> {
}
