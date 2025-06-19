package com.hust.baseweb.applications.education.teacherclassassignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Teacher information.
 */
@Getter
@Setter
@Entity
@Table(name = "teacher")
public class EduTeacher {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "teacher_name")
    private String teacherName;

    @Column(name = "user_login_id")
    private String userLoginId;

    @Column(name = "max_hour_load")
    private double maxHourLoad;

    @Column(name = "created_stamp")
    private Date createdStamp;

    @Column(name = "last_updated_stamp")
    private Date lastUpdatedStamp;

}
