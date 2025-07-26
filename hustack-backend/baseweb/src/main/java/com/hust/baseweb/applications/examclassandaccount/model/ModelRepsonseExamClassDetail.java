package com.hust.baseweb.applications.examclassandaccount.model;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamAccount;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelRepsonseExamClassDetail {

    UUID examClassId;

    String name;

    String description;

    String executeDate;

    List<ExamAccount> accounts;
}
