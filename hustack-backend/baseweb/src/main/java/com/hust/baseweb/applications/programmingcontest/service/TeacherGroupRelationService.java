package com.hust.baseweb.applications.programmingcontest.service;

import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelation;
import com.hust.baseweb.applications.programmingcontest.entity.TeacherGroupRelationId;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationRepository;
import com.hust.baseweb.applications.programmingcontest.repo.TeacherGroupRelationServiceRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeacherGroupRelationService implements TeacherGroupRelationServiceRepo {
    private final TeacherGroupRelationRepository teacherGroupRelationRepository;

    @Autowired
    public TeacherGroupRelationService(TeacherGroupRelationRepository teacherGroupRelationRepository) {
        this.teacherGroupRelationRepository = teacherGroupRelationRepository;
    }

    @Transactional
    public TeacherGroupRelation save(TeacherGroupRelation teacherGroupRelation) {
        return teacherGroupRelationRepository.save(teacherGroupRelation);
    }

    public Optional<TeacherGroupRelation> findById(TeacherGroupRelationId id) {
        return teacherGroupRelationRepository.findById(id);
    }

    public List<TeacherGroupRelation> findAll() {
        return teacherGroupRelationRepository.findAll();
    }

    @Transactional
    public void deleteById(TeacherGroupRelationId id) {
        teacherGroupRelationRepository.deleteById(id);
    }
}
