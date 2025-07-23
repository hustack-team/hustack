package com.hust.baseweb.applications.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "exam_monitor")
public class ExamMonitorEntity {

    @Id
    @Column(length = 60)
    protected String id;

    @Column(name = "exam_result_id")
    private String examResultId;

    @Column(name = "platform")
    private Integer platform;

    @Column(name = "type")
    private Integer type;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "to_time")
    private LocalDateTime toTime;

    @Column(name = "note")
    private String note;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID().toString();
    }
}
