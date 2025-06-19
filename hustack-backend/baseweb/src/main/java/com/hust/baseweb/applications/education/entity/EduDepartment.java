package com.hust.baseweb.applications.education.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class EduDepartment {

    @Id
    private String id;

    @Column(name = "department_name")
    private String name;
}
