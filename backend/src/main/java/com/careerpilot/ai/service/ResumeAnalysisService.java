package com.careerpilot.ai.service;

import com.careerpilot.ai.ai.GeminiService;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.ResumeAnalysis;
import com.careerpilot.ai.repository.ResumeAnalysisRepository;
import com.careerpilot.ai.repository.ResumeRepository;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ResumeAnalysisService {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final GeminiService geminiService;
    private final JsonMapper jsonMapper;

    public ResumeAnalysisService(
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository analysisRepository,
            GeminiService geminiService,
            JsonMapper jsonMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.analysisRepository = analysisRepository;
        this.geminiService = geminiService;
        this.jsonMapper = jsonMapper;
    }

    public ResumeAnalysis analyzeResume(Long resumeId) {

        // Find the uploaded resume
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new RuntimeException("Resume not found")
                );

        // Send resume text to Gemini
        String aiResult = geminiService.analyzeResume(
                resume.getExtractedText()
        );

        try {

            // Parse Gemini response as JSON
            JsonNode jsonNode = jsonMapper.readTree(aiResult);

            // Convert it into clean JSON
            String cleanJson = jsonMapper.writeValueAsString(jsonNode);

            // Find existing analysis or create a new one
            ResumeAnalysis analysis = analysisRepository
                    .findByResumeId(resumeId)
                    .orElseGet(ResumeAnalysis::new);

            // Connect analysis to resume
            analysis.setResume(resume);

            // Save clean JSON
            analysis.setAnalysisJson(cleanJson);

            // Save to database
            return analysisRepository.save(analysis);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid JSON returned by Gemini",
                    e
            );
        }
    }
}