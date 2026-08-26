package com.careerpilot.ai.repository;

import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserOrderByUploadedAtDesc(User user);
}