package com.hust.baseweb.applications.examclassandaccount.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exam_account")
@EntityListeners(AuditingEntityListener.class)
public class ExamAccount {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID Id;

    @Column(name = "exam_class_id")
    private UUID examClassId;

    @Column(name = "random_user_login_id")
    private String randomUserLoginId;

    @Column(name = "password")
    private String password;

    @Column(name = "real_user_login_id")
    private String realUserLoginId;

    @Column(name = "code")
    private String studentCode;

    @Column(name = "fullname")
    private String fullname;

    @Column(name = "status")
    private String status;

    @Column(name = "order_index")
    private Integer orderIndex;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @CreatedDate
    @Column(name = "created_stamp")
    private Date createdStamp;

    @LastModifiedDate
    @Column(name = "last_updated_stamp")
    private Date lastUpdatedStamp;
}
