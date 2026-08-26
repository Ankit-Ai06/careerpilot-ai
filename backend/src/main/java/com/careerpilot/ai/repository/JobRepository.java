package com.careerpilot.ai.repository;

import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByUserOrderByCreatedAtDesc(User user);
}