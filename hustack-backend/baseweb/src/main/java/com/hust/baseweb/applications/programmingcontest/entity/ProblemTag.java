package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "problem_tag")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemTag {

    @EmbeddedId
    private ProblemTagId id;

}
