package com.hust.baseweb.applications.examclassandaccount.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamClassAccountDTO {

    String email;

    String studentCode;

    String fullName;
}
