package com.careerpilot.ai.service;

import com.careerpilot.ai.entity.CareerRoadmap;
import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.User;
import com.careerpilot.ai.exception.ResourceNotFoundException;
import com.careerpilot.ai.repository.CareerRoadmapRepository;
import com.careerpilot.ai.repository.JobMatchRepository;
import com.careerpilot.ai.repository.JobRepository;
import com.careerpilot.ai.repository.ResumeRepository;
import com.careerpilot.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CareerRoadmapRepository roadmapRepository;
    private final ResumeScoreService resumeScoreService;
    private final JsonMapper jsonMapper;

    public DashboardService(
            UserRepository userRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            JobMatchRepository jobMatchRepository,
            CareerRoadmapRepository roadmapRepository,
            ResumeScoreService resumeScoreService,
            JsonMapper jsonMapper
    ) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.roadmapRepository = roadmapRepository;
        this.resumeScoreService = resumeScoreService;
        this.jsonMapper = jsonMapper;
    }

    public Map<String, Object> getDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Resume> resumes = resumeRepository.findByUserOrderByUploadedAtDesc(user);
        List<Job> jobs = jobRepository.findByUserOrderByCreatedAtDesc(user);
        List<CareerRoadmap> roadmaps = roadmapRepository.findByUserOrderByCreatedAtDesc(user);

        Integer latestResumeScore = null;
        if (!resumes.isEmpty()) {
            try {
                Object value = resumeScoreService.calculateScore(resumes.get(0).getId()).get("score");
                if (value instanceof Number number) latestResumeScore = number.intValue();
            } catch (Exception ignored) {
                // A dashboard must remain usable even when an uploaded resume
                // has not yet reached the scoring pipeline.
            }
        }

        List<Map<String, Object>> recentJobs = new ArrayList<>();
        Map<String, Integer> skillGapCounts = new HashMap<>();
        int totalLatestMatches = 0;
        int totalLatestMatchScore = 0;

        for (Job job : jobs) {
            Optional<JobMatch> latest = jobMatchRepository.findFirstByJobOrderByCreatedAtDesc(job);
            Integer matchScore = null;

            if (latest.isPresent()) {
                JobMatch match = latest.get();
                matchScore = match.getOverallScore();
                totalLatestMatches++;
                totalLatestMatchScore += match.getOverallScore();
                collectMissingSkills(match.getMatchJson(), skillGapCounts);
            }

            Map<String, Object> jobMap = new LinkedHashMap<>();
            jobMap.put("id", job.getId());
            jobMap.put("title", job.getTitle());
            jobMap.put("company", job.getCompany());
            jobMap.put("matchScore", matchScore);
            jobMap.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
            recentJobs.add(jobMap);

            if (recentJobs.size() == 5) break;
        }

        int averageJobMatch = totalLatestMatches == 0
                ? 0
                : Math.round((float) totalLatestMatchScore / totalLatestMatches);

        List<Map<String, Object>> topSkillGaps = skillGapCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(6)
                .map(entry -> {
                    Map<String, Object> gap = new LinkedHashMap<>();
                    gap.put("skill", entry.getKey());
                    gap.put("jobCount", entry.getValue());
                    return gap;
                })
                .toList();

        int roadmapItems = 0;
        int completedItems = 0;
        for (CareerRoadmap roadmap : roadmaps) {
            roadmapItems += roadmap.getItems().size();
            completedItems += (int) roadmap.getItems().stream().filter(item -> item.isCompleted()).count();
        }

        int roadmapProgress = roadmapItems == 0
                ? 0
                : Math.round((completedItems * 100f) / roadmapItems);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("resumeScore", latestResumeScore);
        stats.put("averageJobMatch", averageJobMatch);
        stats.put("skillGaps", skillGapCounts.size());
        stats.put("savedJobs", jobs.size());
        stats.put("resumes", resumes.size());
        stats.put("matches", totalLatestMatches);
        stats.put("roadmaps", roadmaps.size());
        stats.put("roadmapProgress", roadmapProgress);

        List<Map<String, Object>> recentResumes = resumes.stream()
                .limit(3)
                .map(resume -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", resume.getId());
                    map.put("fileName", resume.getFileName());
                    map.put("fileType", resume.getFileType());
                    map.put("uploadedAt", resume.getUploadedAt() != null ? resume.getUploadedAt().toString() : null);
                    return map;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", user.getName());
        response.put("stats", stats);
        response.put("topSkillGaps", topSkillGaps);
        response.put("recentJobs", recentJobs);
        response.put("recentResumes", recentResumes);
        return response;
    }

    private void collectMissingSkills(String json, Map<String, Integer> counts) {
        try {
            JsonNode root = jsonMapper.readTree(json);
            JsonNode missingSkills = root.path("missingSkills");
            if (missingSkills.isArray()) {
                for (JsonNode skill : missingSkills) {
                    if (skill.isTextual() && !skill.asText().isBlank()) {
                        String normalized = skill.asText().trim();
                        counts.merge(normalized, 1, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed historical match JSON rather than breaking dashboard loading.
        }
    }
}
