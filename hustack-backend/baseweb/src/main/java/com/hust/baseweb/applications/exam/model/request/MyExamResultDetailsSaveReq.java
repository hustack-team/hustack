package com.hust.baseweb.applications.exam.model.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@FieldNameConstants
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyExamResultDetailsSaveReq {

    String examResultId;
    String examQuestionId;
    Integer questionOrder;
    String filePath;
    String answer;
}
