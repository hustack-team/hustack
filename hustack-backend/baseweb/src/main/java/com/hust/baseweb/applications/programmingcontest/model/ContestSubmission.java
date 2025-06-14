package com.hust.baseweb.applications.programmingcontest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContestSubmission {

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
    
    String codeAuthorship;

    Integer finalSelectedSubmission;

    Integer allowParticipantPinSubmission;
}
