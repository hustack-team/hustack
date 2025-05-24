package com.hust.baseweb.applications.exam.model.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyExamFilterReq {

    String keyword;
    Integer status;
}
