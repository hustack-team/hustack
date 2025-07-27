package com.hust.baseweb.applications.programmingcontest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemDetailForParticipantDTO {

    String problemName;

    String problemCode;

    String problemStatement;

    String createdByUserLoginId;

    String createdByUserFullName;

    String submissionMode;

    Date createdStamp;

    List<AttachmentMetadata> attachments;

    Boolean isPreloadCode;

    String preloadCode;

    List<String> listLanguagesAllowed;

    String sampleTestCase;

    Integer categoryId;

    List<BlockCode> blockCodes;

}
