package com.careerpilot.ai.controller;

import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.ResumeAnalysis;
import com.careerpilot.ai.service.ResumeAnalysisService;
import com.careerpilot.ai.service.ResumeScoreService;
import com.careerpilot.ai.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final ResumeScoreService resumeScoreService;
    private final JsonMapper jsonMapper;

    public ResumeController(
            ResumeService resumeService,
            ResumeAnalysisService resumeAnalysisService,
            ResumeScoreService resumeScoreService,
            JsonMapper jsonMapper
    ) {
        this.resumeService = resumeService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.resumeScoreService = resumeScoreService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        try {
            Resume resume = resumeService.uploadResume(file, authentication.getName());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Resume uploaded successfully");
            response.put("resumeId", resume.getId());
            response.put("fileName", resume.getFileName());
            response.put("fileType", resume.getFileType());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listResumes(Authentication authentication) {
        List<Map<String, Object>> resumes = resumeService
                .getUserResumes(authentication.getName())
                .stream()
                .map(this::toSummary)
                .toList();

        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<?> getResume(
            @PathVariable Long resumeId,
            Authentication authentication
    ) {
        Resume resume = resumeService.getOwnedResume(resumeId, authentication.getName());
        Map<String, Object> map = toSummary(resume);
        map.put("extractedText", resume.getExtractedText());
        return ResponseEntity.ok(map);
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<?> deleteResume(
            @PathVariable Long resumeId,
            Authentication authentication
    ) {
        try {
            resumeService.deleteResume(resumeId, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Resume deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> toSummary(Resume resume) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", resume.getId());
        map.put("fileName", resume.getFileName());
        map.put("fileType", resume.getFileType());
        map.put("uploadedAt", resume.getUploadedAt() != null ? resume.getUploadedAt().toString() : null);
        return map;
    }

    @PostMapping("/{resumeId}/analyze")
    public ResponseEntity<?> analyzeResume(
            @PathVariable Long resumeId,
            Authentication authentication
    ) {
        try {
            resumeService.getOwnedResume(resumeId, authentication.getName());
            ResumeAnalysis analysis = resumeAnalysisService.analyzeResume(resumeId);
            JsonNode analysisJson = jsonMapper.readTree(analysis.getAnalysisJson());

            return ResponseEntity.ok(Map.of(
                    "message", "Resume analyzed successfully",
                    "analysisId", analysis.getId(),
                    "analysis", analysisJson
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{resumeId}/score")
    public ResponseEntity<?> getResumeScore(
            @PathVariable Long resumeId,
            Authentication authentication
    ) {
        try {
            resumeService.getOwnedResume(resumeId, authentication.getName());
            return ResponseEntity.ok(resumeScoreService.calculateScore(resumeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
