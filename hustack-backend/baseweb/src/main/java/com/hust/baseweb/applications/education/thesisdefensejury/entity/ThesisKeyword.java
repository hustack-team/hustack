package com.hust.baseweb.applications.education.thesisdefensejury.entity;


import com.hust.baseweb.applications.education.thesisdefensejury.composite.ThesisKeywordID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@Table(name = "thesis_keyword") // Entity map voi bang thesis_keyword
@IdClass(ThesisKeywordID.class)
@NoArgsConstructor
public class ThesisKeyword {

    @Id
    @Column(name = "thesis_id")
    private UUID thesis;


    @Column(name = "keyword")
    private String keyword;

}
