package com.careerpilot.ai.service;

import com.careerpilot.ai.ai.GeminiService;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.ResumeAnalysis;
import com.careerpilot.ai.repository.ResumeAnalysisRepository;
import com.careerpilot.ai.repository.ResumeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ResumeAnalysis analyzeResume(Long resumeId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new RuntimeException("Resume not found")
                );

        // Force-load the user while the Hibernate session is active.
        resume.getUser().getId();

        String extractedText = resume.getExtractedText();

        if (extractedText == null || extractedText.isBlank()) {
            throw new RuntimeException(
                    "Resume text could not be extracted"
            );
        }

        // Send resume text to Gemini.
        String aiResult = geminiService.analyzeResume(extractedText);

        try {

            JsonNode jsonNode = jsonMapper.readTree(aiResult);

            if (jsonNode == null) {
                throw new RuntimeException(
                        "Gemini returned an empty response"
                );
            }

            String cleanJson =
                    jsonMapper.writeValueAsString(jsonNode);

            ResumeAnalysis analysis =
                    analysisRepository
                            .findByResumeId(resumeId)
                            .orElseGet(ResumeAnalysis::new);

            analysis.setResume(resume);
            analysis.setAnalysisJson(cleanJson);

            return analysisRepository.save(analysis);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid JSON returned by Gemini: "
                            + e.getMessage(),
                    e
            );
        }
    }
}