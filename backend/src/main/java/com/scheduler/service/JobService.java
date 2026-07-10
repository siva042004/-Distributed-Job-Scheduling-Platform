package com.scheduler.service;

import com.scheduler.dto.JobRequest;
import com.scheduler.kafka.JobEventProducer;
import com.scheduler.model.Job;
import com.scheduler.repository.JobRepository;
import com.scheduler.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventProducer jobEventProducer;
    private final NotificationService notificationService;

    public Job createJob(JobRequest request) {
        Job job = new Job();
        job.setName(request.getName());
        job.setJobType(request.getJobType());
        job.setPayload(request.getPayload());
        job.setScheduledAt(request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now());
        job.setMaxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3);
        job.setStatus(Job.JobStatus.PENDING);

        Job saved = jobRepository.save(job);
        notificationService.broadcastJobUpdate(saved);
        log.info("Created job {} ({})", saved.getId(), saved.getName());
        return saved;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    public Job cancelJob(Long id) {
        Job job = getJob(id);
        job.setStatus(Job.JobStatus.CANCELLED);
        Job saved = jobRepository.save(job);
        notificationService.broadcastJobUpdate(saved);
        return saved;
    }

    public Job retryJob(Long id) {
        Job job = getJob(id);
        job.setStatus(Job.JobStatus.PENDING);
        job.setRetryCount(0);
        job.setLastError(null);
        Job saved = jobRepository.save(job);
        jobEventProducer.publishJobEvent(saved);
        return saved;
    }

    // Polls for due jobs every 5 seconds and dispatches them to Kafka for distributed processing
    @Scheduled(fixedDelay = 5000)
    public void dispatchDueJobs() {
        List<Job> dueJobs = jobRepository.findDueJobs();
        for (Job job : dueJobs) {
            job.setStatus(Job.JobStatus.SCHEDULED);
            jobRepository.save(job);
            jobEventProducer.publishJobEvent(job);
            log.info("Dispatched job {} to Kafka for processing", job.getId());
        }
    }

    // Publishes aggregate metrics every 3 seconds for dashboard monitoring
    @Scheduled(fixedDelay = 3000)
    public void publishMetrics() {
        Map<String, Object> metrics = Map.of(
                "pending", jobRepository.countByStatus(Job.JobStatus.PENDING),
                "running", jobRepository.countByStatus(Job.JobStatus.RUNNING),
                "completed", jobRepository.countByStatus(Job.JobStatus.COMPLETED),
                "failed", jobRepository.countByStatus(Job.JobStatus.FAILED),
                "retrying", jobRepository.countByStatus(Job.JobStatus.RETRYING),
                "timestamp", LocalDateTime.now().toString()
        );
        notificationService.broadcastMetrics(metrics);
    }
}
