package com.hust.baseweb.applications.programmingcontest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelContestSubmitProgramViaUploadFile {

    String contestId;

    String problemId;

    String language;

    String userId;

    String submittedByUserId;

    List<BlockCode> blockCodes;

}
