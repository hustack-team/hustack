package com.hust.baseweb.applications.exam.model.response;

import com.hust.baseweb.applications.exam.entity.ExamQuestionEntity;
import com.hust.baseweb.applications.exam.entity.ExamTagEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
public class ExamQuestionFilterRes extends ExamQuestionEntity {

    private String examSubjectName;
    private List<ExamTagEntity> examTags;
}
