package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProblemBlockCode {
    private Long id;
    private Integer seq;
    private String completedBy;
    private String sourceCode;
    private String programmingLanguage;
    private Boolean isForStudent;
}
