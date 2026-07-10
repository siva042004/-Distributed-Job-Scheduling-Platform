package com.scheduler.websocket;

import com.scheduler.model.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastJobUpdate(Job job) {
        messagingTemplate.convertAndSend("/topic/jobs", job);
    }

    public void broadcastFailureAlert(Job job, String reason) {
        messagingTemplate.convertAndSend("/topic/alerts", Map.of(
                "jobId", job.getId(),
                "jobName", job.getName(),
                "status", job.getStatus(),
                "reason", reason,
                "retryCount", job.getRetryCount()
        ));
    }

    public void broadcastMetrics(Map<String, Object> metrics) {
        messagingTemplate.convertAndSend("/topic/metrics", metrics);
    }
}
