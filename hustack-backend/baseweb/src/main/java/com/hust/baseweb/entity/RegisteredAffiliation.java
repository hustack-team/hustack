package com.hust.baseweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "registered_affiliation")
public class RegisteredAffiliation {

    @Id
    @Column(name = "affiliation_id")
    private String affiliationId;

    @Column(name = "affiliation_name")
    private String affiliationName;

}
