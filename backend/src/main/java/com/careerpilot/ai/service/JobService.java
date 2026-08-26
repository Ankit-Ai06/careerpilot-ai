package com.careerpilot.ai.service;

import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.User;
import com.careerpilot.ai.exception.ForbiddenException;
import com.careerpilot.ai.exception.ResourceNotFoundException;
import com.careerpilot.ai.repository.JobRepository;
import com.careerpilot.ai.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public JobService(
            JobRepository jobRepository,
            UserRepository userRepository
    ) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    public Job createJob(
            String email,
            String title,
            String company,
            String jobUrl,
            String description
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Job title is required"
            );
        }

        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException(
                    "Company name is required"
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Job description is required"
            );
        }

        Job job = new Job();

        job.setUser(user);
        job.setTitle(title.trim());
        job.setCompany(company.trim());

        if (jobUrl != null && !jobUrl.isBlank()) {
            job.setJobUrl(jobUrl.trim());
        }

        job.setDescription(description.trim());

        return jobRepository.save(job);
    }

    public List<Job> getUserJobs(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return jobRepository
                .findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Fetches a job and verifies the requesting user owns it.
     */
    public Job getOwnedJob(Long jobId, String email) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found")
                );

        if (!job.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("You don't have access to this job");
        }

        return job;
    }

    public void deleteJob(Long jobId, String email) {
        Job job = getOwnedJob(jobId, email);
        jobRepository.delete(job);
    }
}