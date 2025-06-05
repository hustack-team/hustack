package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationServiceRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherGroupRelationService implements TeacherGroupRelationServiceRepo {
    TeacherGroupRelationRepository teacherGroupRelationRepository;

    @Transactional
    public TeacherGroupRelation save(TeacherGroupRelation teacherGroupRelation) {
        return teacherGroupRelationRepository.save(teacherGroupRelation);
    }

    @Transactional(readOnly = true)
    public Optional<TeacherGroupRelation> findById(TeacherGroupRelationId id) {
        return teacherGroupRelationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<TeacherGroupRelation> findAll() {
        return teacherGroupRelationRepository.findAll();
    }

    @Transactional
    public void deleteById(TeacherGroupRelationId id) {
        teacherGroupRelationRepository.deleteById(id);
    }
}
