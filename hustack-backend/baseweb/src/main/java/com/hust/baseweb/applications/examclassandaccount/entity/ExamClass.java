package com.hust.baseweb.applications.examclassandaccount.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exam_class")
public class ExamClass {

    public static final String STATUS_ACTIVE = "ACTIVE";

    public static final String STATUS_DISABLE = "DISABLED";

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID Id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "execute_date")
    private String executeDate;

    @Column(name = "created_by_user_id")
    private String createdByUserId;

}
