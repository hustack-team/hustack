package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestSubmission {

    @Id
    UUID contestSubmissionId;

    String problemId;

    String problemName;

    String contestId;

    String userId;

    String fullname;

    String testCasePass;

    String sourceCodeLanguage;

    Long point;

    String status;

    String message;

    String createAt;

    String managementStatus;

    String violationForbiddenInstruction;

    String violationForbiddenInstructionMessage;

    String createdByIp;

    @Column(name = "final_selected_submission", length = 1)
    private String finalSelectedSubmission;

    String codeAuthorship;
}
