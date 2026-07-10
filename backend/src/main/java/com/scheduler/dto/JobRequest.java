package com.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String jobType;

    private String payload;

    private LocalDateTime scheduledAt;

    private Integer maxRetries;
}
