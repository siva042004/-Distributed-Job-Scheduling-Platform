package com.scheduler.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.Job;
import com.scheduler.repository.JobRepository;
import com.scheduler.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobEventConsumer {

    private final JobRepository jobRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // concurrency = 3 configured in KafkaConsumerConfig -> handles concurrent processing across worker threads
    @KafkaListener(topics = JobEventProducer.TOPIC, groupId = "job-scheduler-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            Job jobSnapshot = objectMapper.readValue(record.value(), Job.class);
            processJobWithRetry(jobSnapshot.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process kafka record at offset {}: {}", record.offset(), e.getMessage());
            // Do not ack -> message will be redelivered
        }
    }

    @Retryable(
            retryFor = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void processJobWithRetry(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        String worker = "worker-" + (ThreadLocalRandom.current().nextInt(1, 4));
        job.setAssignedWorker(worker);
        job.setStatus(Job.JobStatus.RUNNING);
        jobRepository.save(job);
        notificationService.broadcastJobUpdate(job);

        try {
            simulateWork(job);
            job.setStatus(Job.JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            notificationService.broadcastJobUpdate(job);
            log.info("Job {} completed successfully by {}", job.getId(), worker);
        } catch (Exception ex) {
            job.setRetryCount(job.getRetryCount() + 1);
            job.setLastError(ex.getMessage());

            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus(Job.JobStatus.FAILED);
                jobRepository.save(job);
                notificationService.broadcastJobUpdate(job);
                notificationService.broadcastFailureAlert(job, ex.getMessage());
                log.error("Job {} permanently failed after {} retries", job.getId(), job.getRetryCount());
                return; // stop retrying, don't rethrow
            }

            job.setStatus(Job.JobStatus.RETRYING);
            jobRepository.save(job);
            notificationService.broadcastJobUpdate(job);
            // rethrow so @Retryable triggers backoff + retry
            throw new RuntimeException("Job " + job.getId() + " failed, will retry: " + ex.getMessage(), ex);
        }
    }

    // Simulates unreliable work: ~30% failure rate to exercise retry logic
    private void simulateWork(Job job) throws InterruptedException {
        Thread.sleep(500);
        if (new Random().nextInt(100) < 30) {
            throw new RuntimeException("Simulated transient failure while processing job " + job.getId());
        }
    }
}
