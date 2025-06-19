package com.hust.baseweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class RoleType {

    @Id
    @Column(name = "role_type_id")
    private String roleTypeId;

}
