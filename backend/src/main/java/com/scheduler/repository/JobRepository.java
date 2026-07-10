package com.scheduler.repository;

import com.scheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(Job.JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = 'PENDING' AND j.scheduledAt <= CURRENT_TIMESTAMP")
    List<Job> findDueJobs();

    long countByStatus(Job.JobStatus status);
}
