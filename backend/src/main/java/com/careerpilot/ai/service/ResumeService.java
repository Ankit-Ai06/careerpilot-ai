package com.careerpilot.ai.service;

import com.careerpilot.ai.entity.CareerRoadmap;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.User;
import com.careerpilot.ai.exception.ForbiddenException;
import com.careerpilot.ai.exception.ResourceNotFoundException;
import com.careerpilot.ai.parser.ResumeParser;
import com.careerpilot.ai.repository.CareerRoadmapRepository;
import com.careerpilot.ai.repository.JobMatchRepository;
import com.careerpilot.ai.repository.ResumeAnalysisRepository;
import com.careerpilot.ai.repository.ResumeRepository;
import com.careerpilot.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeParser resumeParser;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CareerRoadmapRepository careerRoadmapRepository;

    public ResumeService(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            ResumeParser resumeParser,
            ResumeAnalysisRepository resumeAnalysisRepository,
            JobMatchRepository jobMatchRepository,
            CareerRoadmapRepository careerRoadmapRepository
    ) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeParser = resumeParser;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.careerRoadmapRepository = careerRoadmapRepository;
    }

    public Resume uploadResume(MultipartFile file, String email) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a resume file");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Resume must be smaller than 5 MB");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
            throw new IllegalArgumentException("Only PDF and DOCX files are allowed");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String extractedText = cleanExtractedText(resumeParser.extractText(file));
        if (extractedText.isBlank()) {
            throw new IllegalArgumentException("Could not extract text from this resume");
        }

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(fileName);
        resume.setFileType(lowerName.endsWith(".pdf") ? "PDF" : "DOCX");
        resume.setExtractedText(extractedText);

        return resumeRepository.save(resume);
    }

    public List<Resume> getUserResumes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return resumeRepository.findByUserOrderByUploadedAtDesc(user);
    }

    public Resume getOwnedResume(Long resumeId, String email) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        if (!resume.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("You don't have access to this resume");
        }

        return resume;
    }

    @Transactional
    public void deleteResume(Long resumeId, String email) {
        Resume resume = getOwnedResume(resumeId, email);

        // Remove dependent roadmap data first because a roadmap belongs to a job match.
        List<JobMatch> matches = jobMatchRepository.findByResumeOrderByCreatedAtDesc(resume);
        for (JobMatch match : matches) {
            List<CareerRoadmap> roadmaps = careerRoadmapRepository.findByJobMatch(match);
            if (!roadmaps.isEmpty()) {
                careerRoadmapRepository.deleteAll(roadmaps);
            }
        }

        // Remove match results and AI analysis before the resume itself.
        if (!matches.isEmpty()) {
            jobMatchRepository.deleteAll(matches);
        }

        resumeAnalysisRepository.deleteByResumeId(resumeId);
        resumeRepository.delete(resume);
    }

    private String cleanExtractedText(String text) {
        return text
                .replace("\u0000", "")
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .trim();
    }
}
