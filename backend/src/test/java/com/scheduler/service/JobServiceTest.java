package com.scheduler.service;

import com.scheduler.dto.JobRequest;
import com.scheduler.kafka.JobEventProducer;
import com.scheduler.model.Job;
import com.scheduler.repository.JobRepository;
import com.scheduler.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobEventProducer jobEventProducer;
    @Mock private NotificationService notificationService;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jobService = new JobService(jobRepository, jobEventProducer, notificationService);
    }

    @Test
    void createJobPersistsWithPendingStatus() {
        JobRequest request = new JobRequest();
        request.setName("Send Report");
        request.setJobType("REPORT");

        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(100L);
            return j;
        });

        Job result = jobService.createJob(request);

        assertEquals(Job.JobStatus.PENDING, result.getStatus());
        assertEquals("Send Report", result.getName());
        verify(notificationService).broadcastJobUpdate(result);
    }

    @Test
    void cancelJobSetsCancelledStatus() {
        Job existing = new Job();
        existing.setId(5L);
        existing.setStatus(Job.JobStatus.PENDING);

        when(jobRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = jobService.cancelJob(5L);

        assertEquals(Job.JobStatus.CANCELLED, result.getStatus());
    }

    @Test
    void retryJobResetsRetryCountAndDispatches() {
        Job existing = new Job();
        existing.setId(7L);
        existing.setRetryCount(3);
        existing.setStatus(Job.JobStatus.FAILED);

        when(jobRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        Job result = jobService.retryJob(7L);

        assertEquals(0, result.getRetryCount());
        assertEquals(Job.JobStatus.PENDING, result.getStatus());
        verify(jobEventProducer).publishJobEvent(result);
    }

    @Test
    void getJobThrowsWhenNotFound() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> jobService.getJob(999L));
    }
}
