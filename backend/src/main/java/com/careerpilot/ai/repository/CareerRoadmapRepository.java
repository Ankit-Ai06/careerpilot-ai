package com.careerpilot.ai.repository;

import com.careerpilot.ai.entity.CareerRoadmap;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareerRoadmapRepository extends JpaRepository<CareerRoadmap, Long> {

    List<CareerRoadmap> findByUserOrderByCreatedAtDesc(User user);

    Optional<CareerRoadmap> findFirstByJobMatch(JobMatch jobMatch);

    List<CareerRoadmap> findByJobMatch(JobMatch jobMatch);
}
