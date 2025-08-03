package com.hust.baseweb.applications.examclassandaccount.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamClassAccountStatusUpdateRequestDTO {

    boolean enabled;
}
