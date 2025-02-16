package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemTagId implements Serializable {

    @Column(name = "tag_id")
    Integer tagId;

    @Column(name = "problem_id")
    String problemId;

}
