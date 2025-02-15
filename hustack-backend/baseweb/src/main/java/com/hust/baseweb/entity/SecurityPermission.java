package com.hust.baseweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class SecurityPermission {

    @Id
    @Column(name = "permission_id")
    private String permissionId;

    private String description;


    private Date createdStamp;


    private Date lastUpdatedStamp;

    public SecurityPermission() {
    }
}
