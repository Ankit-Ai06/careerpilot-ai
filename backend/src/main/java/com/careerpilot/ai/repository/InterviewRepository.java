package com.careerpilot.ai.repository;

import com.careerpilot.ai.entity.Interview;
import com.careerpilot.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserOrderByCreatedAtDesc(User user);

    Optional<Interview> findByIdAndUser(Long id, User user);
}