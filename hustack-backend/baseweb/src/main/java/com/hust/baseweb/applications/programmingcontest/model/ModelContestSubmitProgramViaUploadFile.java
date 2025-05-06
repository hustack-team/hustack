package com.hust.baseweb.applications.programmingcontest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelContestSubmitProgramViaUploadFile {

    String contestId;

    String problemId;

    String language;

    String userId;

    String submittedByUserId;
}
