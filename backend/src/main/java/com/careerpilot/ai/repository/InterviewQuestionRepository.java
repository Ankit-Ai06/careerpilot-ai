package com.careerpilot.ai.repository;

import com.careerpilot.ai.entity.Interview;
import com.careerpilot.ai.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository
        extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByInterviewOrderByQuestionNumberAsc(
            Interview interview
    );

    Optional<InterviewQuestion> findByInterviewAndQuestionNumber(
            Interview interview,
            Integer questionNumber
    );
}