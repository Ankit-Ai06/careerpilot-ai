package com.careerpilot.ai.repository;

import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {

    Optional<JobMatch> findFirstByJobOrderByCreatedAtDesc(Job job);

    List<JobMatch> findByJobOrderByCreatedAtDesc(Job job);

    List<JobMatch> findByResumeOrderByCreatedAtDesc(Resume resume);
}
