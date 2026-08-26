package com.careerpilot.ai.controller;

import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.service.JobMatchService;
import com.careerpilot.ai.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobMatchService jobMatchService;
    private final JsonMapper jsonMapper;

    public JobController(
            JobService jobService,
            JobMatchService jobMatchService,
            JsonMapper jsonMapper
    ) {
        this.jobService = jobService;
        this.jobMatchService = jobMatchService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping
    public ResponseEntity<?> createJob(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {

        try {

            Job job = jobService.createJob(
                    authentication.getName(),
                    request.get("title"),
                    request.get("company"),
                    request.get("jobUrl"),
                    request.get("description")
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Job saved successfully",
                            "jobId", job.getId(),
                            "title", job.getTitle(),
                            "company", job.getCompany()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message", e.getMessage()
                    )
            );
        }
    }

    @GetMapping
    public ResponseEntity<?> getJobs(
            Authentication authentication
    ) {

        List<Job> jobs =
                jobService.getUserJobs(authentication.getName());

        List<Map<String, Object>> response =
                jobs.stream().map(this::toSummary).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJob(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        Job job = jobService.getOwnedJob(jobId, authentication.getName());
        return ResponseEntity.ok(toDetail(job));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        jobService.deleteJob(jobId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Job deleted"));
    }


    // ==========================================
    // RESUME <-> JOB MATCHING
    // ==========================================

    @PostMapping("/{jobId}/match")
    public ResponseEntity<?> runMatch(
            @PathVariable Long jobId,
            @RequestBody Map<String, Long> request,
            Authentication authentication
    ) {
        Long resumeId = request.get("resumeId");

        if (resumeId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "resumeId is required")
            );
        }

        JobMatch match = jobMatchService.runMatch(
                jobId, resumeId, authentication.getName()
        );

        return ResponseEntity.ok(toMatchResponse(match));
    }

    @GetMapping("/{jobId}/match")
    public ResponseEntity<?> getLatestMatch(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        JobMatch match = jobMatchService.getLatestMatch(
                jobId, authentication.getName()
        );

        return ResponseEntity.ok(toMatchResponse(match));
    }


    // ==========================================
    // MAPPERS - never return JPA entities directly
    // (they carry the related User, including the
    // password hash, straight into the JSON response)
    // ==========================================

    private Map<String, Object> toSummary(Job job) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("company", job.getCompany());
        map.put("jobUrl", job.getJobUrl());
        map.put("createdAt", job.getCreatedAt().toString());
        return map;
    }

    private Map<String, Object> toDetail(Job job) {
        Map<String, Object> map = toSummary(job);
        map.put("description", job.getDescription());
        return map;
    }

    private Map<String, Object> toMatchResponse(JobMatch match) {
        JsonNode analysisJson = jsonMapper.readTree(match.getMatchJson());

        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("matchId", match.getId());
        map.put("jobId", match.getJob().getId());
        map.put("resumeId", match.getResume().getId());
        map.put("overallScore", match.getOverallScore());
        map.put("createdAt", match.getCreatedAt().toString());
        map.put("analysis", analysisJson);
        return map;
    }
}