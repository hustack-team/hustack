package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
public class ContestSubmissionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID submissionId;
    private String userId;
    private String comment;
    private Date createdStamp;
    private Date lastUpdatedStamp;
}
