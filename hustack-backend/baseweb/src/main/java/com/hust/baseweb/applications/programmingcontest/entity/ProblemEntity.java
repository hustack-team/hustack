package com.hust.baseweb.applications.programmingcontest.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "contest_problem_new")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProblemEntity implements Serializable {

    private static final long serialVersionUID = 3487495895819800L;

    public static final String PROBLEM_STATUS_OPEN = "OPEN";

    public static final String PROBLEM_STATUS_HIDDEN = "HIDDEN";

    @Id
    @Column(name = "problem_id")
    String problemId;

    @Column(name = "problem_name", unique = true)
    String problemName;

    @Column(name = "problem_description")
    String problemDescription;

    @Column(name = "time_limit_cpp")
    float timeLimitCPP;

    @Column(name = "time_limit_java")
    float timeLimitJAVA;

    @Column(name = "time_limit_python")
    float timeLimitPYTHON;

    @Column(name = "memory_limit")
    float memoryLimit;

    @Column(name = "level_id")
    String levelId;

    @Column(name = "category_id")
    Integer categoryId;

    @Column(name = "correct_solution_source_code")
    String correctSolutionSourceCode;

    @Column(name = "correct_solution_language")
    String correctSolutionLanguage;

    @Column(name = "solution_checker_source_code")
    String solutionCheckerSourceCode;

    @Column(name = "solution_checker_source_language")
    String solutionCheckerSourceLanguage;

    @Column(name = "is_public")
    boolean isPublicProblem;

    @Column(name = "attachment")
    String attachment;

    @Column(name = "score_evaluation_type")
    String scoreEvaluationType;

    @Column(name = "appearances")
    int appearances;

    @Column(name = "is_preload_code")
    Boolean isPreloadCode;

    @Column(name = "preload_code")
    String preloadCode;

    @JoinTable(name = "problem_tag",
               joinColumns = @JoinColumn(name = "problem_id", referencedColumnName = "problem_id"),
               inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "tag_id")
    )
    @OneToMany(fetch = FetchType.LAZY)
    List<TagEntity> tags;

    @Column(name = "status_id")
    String statusId;

    @Column(name = "sample_testcase")
    String sampleTestcase;

    @CreatedDate
    @Column(name = "created_stamp")
    Date createdAt;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    String lastModifiedBy;

    @LastModifiedDate
    Date lastUpdatedStamp;

    @CreatedBy
    @Column(name = "created_by_user_login_id")
    String createdBy;
}
