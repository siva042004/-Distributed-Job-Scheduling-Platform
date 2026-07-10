package com.scheduler.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobEventProducer {

    public static final String TOPIC = "job-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void publishJobEvent(Job job) {
        try {
            String payload = objectMapper.writeValueAsString(job);
            kafkaTemplate.send(TOPIC, String.valueOf(job.getId()), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish job event for job {}: {}", job.getId(), ex.getMessage());
                        } else {
                            log.info("Published job event for job {} to partition {}", job.getId(),
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.error("Error serializing job {}: {}", job.getId(), e.getMessage());
        }
    }
}
