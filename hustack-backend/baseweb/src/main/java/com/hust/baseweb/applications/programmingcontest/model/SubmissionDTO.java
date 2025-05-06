package com.hust.baseweb.applications.programmingcontest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionDTO {

    UUID contestSubmissionId;

    String problemId;

    String contestId;

    String userId;

    String testCasePass;

    String sourceCode;

    String sourceCodeLanguage;

    Long runtime;

    float memoryUsage;

    Long point;

    String status;

    String managementStatus;

    String submittedByUserId;

    Date createdAt;

//    Date updateAt;

//    String lastUpdatedByUserId;

    String message;

//    String violateForbiddenInstruction;

//    String violateForbiddenInstructionMessage;
}
