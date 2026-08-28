package com.careerpilot.ai.service;

import com.careerpilot.ai.ai.GeminiService;
import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.exception.ResourceNotFoundException;
import com.careerpilot.ai.repository.JobMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class JobMatchService {

    private final JobService jobService;
    private final ResumeService resumeService;
    private final GeminiService geminiService;
    private final JobMatchRepository jobMatchRepository;
    private final JsonMapper jsonMapper;

    public JobMatchService(
            JobService jobService,
            ResumeService resumeService,
            GeminiService geminiService,
            JobMatchRepository jobMatchRepository,
            JsonMapper jsonMapper
    ) {
        this.jobService = jobService;
        this.resumeService = resumeService;
        this.geminiService = geminiService;
        this.jobMatchRepository = jobMatchRepository;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Runs a fresh AI match between a job (owned by the caller) and a
     * resume (also owned by the caller), then stores and returns it.
     */
    @Transactional
    public JobMatch runMatch(Long jobId, Long resumeId, String email) {

        Job job = jobService.getOwnedJob(jobId, email);
        Resume resume = resumeService.getOwnedResume(resumeId, email);

        if (resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
            throw new IllegalArgumentException("This resume has no extracted text to match against");
        }

        String aiResult = geminiService.matchResumeToJob(
                resume.getExtractedText(),
                job.getDescription()
        );

        try {
            JsonNode jsonNode = jsonMapper.readTree(aiResult);
            String cleanJson = jsonMapper.writeValueAsString(jsonNode);

            int overallScore = 0;
            JsonNode scoreNode = jsonNode.get("overallScore");
            if (scoreNode != null && scoreNode.isNumber()) {
                overallScore = Math.max(0, Math.min(100, scoreNode.asInt()));
            }

            JobMatch match = new JobMatch();
            match.setJob(job);
            match.setResume(resume);
            match.setOverallScore(overallScore);
            match.setMatchJson(cleanJson);

            return jobMatchRepository.save(match);

        } catch (Exception e) {
            throw new RuntimeException("The AI returned an invalid response. Please try again.", e);
        }
    }

    public JobMatch getLatestMatch(Long jobId, String email) {

        Job job = jobService.getOwnedJob(jobId, email);

        return jobMatchRepository.findFirstByJobOrderByCreatedAtDesc(job)
                .orElseThrow(() ->
                        new ResourceNotFoundException("No match has been run for this job yet")
                );
    }
}
