package com.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String jobType; // e.g. EMAIL, REPORT, DATA_SYNC

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int maxRetries = 3;

    private int retryCount = 0;

    private LocalDateTime scheduledAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private String assignedWorker;

    public enum JobStatus {
        PENDING, SCHEDULED, RUNNING, RETRYING, COMPLETED, FAILED, CANCELLED
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
