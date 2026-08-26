package com.careerpilot.ai.service;

import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.ResumeAnalysis;
import com.careerpilot.ai.repository.ResumeAnalysisRepository;
import com.careerpilot.ai.repository.ResumeRepository;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ResumeScoreService {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final JsonMapper jsonMapper;

    public ResumeScoreService(
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository analysisRepository,
            JsonMapper jsonMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.analysisRepository = analysisRepository;
        this.jsonMapper = jsonMapper;
    }

    public Map<String, Object> calculateScore(Long resumeId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new RuntimeException("Resume not found")
                );

        String text = resume.getExtractedText();

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Resume text is empty");
        }

        String resumeText = text.toLowerCase();

        /*
         * ==========================================
         * CATEGORY SCORES
         * ==========================================
         */

        int contactScore = calculateContactScore(resumeText);

        int structureScore = calculateStructureScore(resumeText);

        int skillsScore = calculateSkillsScore(resumeText);

        int projectScore = calculateProjectScore(resumeText);

        int educationScore = calculateEducationScore(resumeText);

        int experienceScore = calculateExperienceScore(resumeText);

        int achievementScore = calculateAchievementScore(resumeText);

        int keywordScore = calculateKeywordScore(resumeId, resumeText);

        int formattingScore = calculateFormattingScore(text, resumeText);

        /*
         * ==========================================
         * TOTAL SCORE
         * ==========================================
         */

        int totalScore =
                contactScore +
                structureScore +
                skillsScore +
                projectScore +
                educationScore +
                experienceScore +
                achievementScore +
                keywordScore +
                formattingScore;

        totalScore = Math.min(totalScore, 100);

        /*
         * ==========================================
         * RATING
         * ==========================================
         */

        String rating;

        if (totalScore >= 85) {

            rating = "Excellent Resume";

        } else if (totalScore >= 70) {

            rating = "Good Resume";

        } else if (totalScore >= 50) {

            rating = "Needs Improvement";

        } else {

            rating = "Weak Resume";
        }

        /*
         * ==========================================
         * BREAKDOWN
         * ==========================================
         */

        Map<String, Integer> breakdown =
                new LinkedHashMap<>();

        breakdown.put(
                "contactInformation",
                contactScore
        );

        breakdown.put(
                "resumeStructure",
                structureScore
        );

        breakdown.put(
                "skills",
                skillsScore
        );

        breakdown.put(
                "projects",
                projectScore
        );

        breakdown.put(
                "education",
                educationScore
        );

        breakdown.put(
                "experience",
                experienceScore
        );

        breakdown.put(
                "achievements",
                achievementScore
        );

        breakdown.put(
                "keywords",
                keywordScore
        );

        breakdown.put(
                "formattingReadability",
                formattingScore
        );

        /*
         * ==========================================
         * FINAL RESPONSE
         * ==========================================
         */

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "resumeId",
                resumeId
        );

        result.put(
                "score",
                totalScore
        );

        result.put(
                "rating",
                rating
        );

        result.put(
                "breakdown",
                breakdown
        );

        return result;
    }


    // ==========================================
    // CONTACT INFORMATION - 5 POINTS
    // ==========================================

    private int calculateContactScore(String text) {

        int score = 0;

        boolean hasEmail =
                text.matches(
                        "(?s).*\\b[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}\\b.*"
                );

        boolean hasPhone =
                text.matches(
                        "(?s).*\\b[6-9]\\d{9}\\b.*"
                );

        if (hasEmail) {
            score += 3;
        }

        if (hasPhone) {
            score += 2;
        }

        return score;
    }


    // ==========================================
    // STRUCTURE - 10 POINTS
    // ==========================================

    private int calculateStructureScore(String text) {

        int score = 0;

        if (containsAny(
                text,
                "education",
                "academic"
        )) {
            score += 2;
        }

        if (containsAny(
                text,
                "skills",
                "technical skills"
        )) {
            score += 2;
        }

        if (containsAny(
                text,
                "project",
                "projects"
        )) {
            score += 2;
        }

        if (containsAny(
                text,
                "experience",
                "internship"
        )) {
            score += 2;
        }

        if (containsAny(
                text,
                "certification",
                "certifications",
                "achievement",
                "achievements"
        )) {
            score += 2;
        }

        return Math.min(score, 10);
    }


    // ==========================================
    // SKILLS - 15 POINTS
    // ==========================================

    private int calculateSkillsScore(String text) {

        int skillCount = countSkills(text);

        if (skillCount >= 15) {
            return 15;
        }

        if (skillCount >= 12) {
            return 14;
        }

        if (skillCount >= 9) {
            return 12;
        }

        if (skillCount >= 6) {
            return 9;
        }

        if (skillCount >= 3) {
            return 6;
        }

        if (skillCount > 0) {
            return 3;
        }

        return 0;
    }


    // ==========================================
    // PROJECTS - 15 POINTS
    // ==========================================

    private int calculateProjectScore(String text) {

        int projectCount =
                estimateProjectCount(text);

        if (projectCount >= 4) {
            return 15;
        }

        if (projectCount == 3) {
            return 13;
        }

        if (projectCount == 2) {
            return 10;
        }

        if (projectCount == 1) {
            return 7;
        }

        return 0;
    }


    // ==========================================
    // EDUCATION - 10 POINTS
    // ==========================================

    private int calculateEducationScore(String text) {

        int score = 0;

        boolean hasDegree =
                containsAny(
                        text,
                        "b.tech",
                        "btech",
                        "bachelor",
                        "b.sc",
                        "bca",
                        "m.tech",
                        "master"
                );

        boolean hasInstitution =
                containsAny(
                        text,
                        "university",
                        "college",
                        "institute"
                );

        boolean hasGraduationYear =
                text.matches(
                        "(?s).*\\b20(2[0-9]|3[0-9])\\b.*"
                );

        if (hasDegree) {
            score += 5;
        }

        if (hasInstitution) {
            score += 3;
        }

        if (hasGraduationYear) {
            score += 2;
        }

        return Math.min(score, 10);
    }


    // ==========================================
    // EXPERIENCE - 15 POINTS
    // ==========================================

    private int calculateExperienceScore(String text) {

        /*
         * IMPORTANT:
         *
         * We don't simply check whether the word
         * "experience" exists.
         *
         * We look for actual employment/internship
         * indicators.
         */

        boolean hasInternship =
                containsAny(
                        text,
                        "intern at",
                        "interned at",
                        "software engineer intern",
                        "developer intern",
                        "web development intern",
                        "internship at"
                );

        boolean hasJob =
                containsAny(
                        text,
                        "software engineer",
                        "software developer",
                        "web developer",
                        "full stack developer",
                        "frontend developer",
                        "backend developer"
                );

        boolean hasCompany =
                containsAny(
                        text,
                        "technologies pvt",
                        "technologies ltd",
                        "private limited",
                        "pvt ltd",
                        "inc.",
                        "corp."
                );

        if (hasInternship && hasJob) {
            return 15;
        }

        if (hasInternship) {
            return 10;
        }

        if (hasJob && hasCompany) {
            return 12;
        }

        /*
         * Projects are not professional experience.
         *
         * We intentionally don't award 15 points just
         * because the resume contains the word
         * "experience".
         */

        return 0;
    }


    // ==========================================
    // ACHIEVEMENTS - 10 POINTS
    // ==========================================

    private int calculateAchievementScore(String text) {

        int score = 0;

        boolean hasCertification =
                containsAny(
                        text,
                        "certification",
                        "certifications",
                        "certified"
                );

        boolean hasHackathon =
                containsAny(
                        text,
                        "hackathon",
                        "hackathons"
                );

        boolean hasCodingPlatform =
                containsAny(
                        text,
                        "leetcode",
                        "geeksforgeeks",
                        "codeforces",
                        "coding ninjas"
                );

        boolean hasAward =
                containsAny(
                        text,
                        "award",
                        "achievement",
                        "achievements"
                );

        if (hasCertification) {
            score += 3;
        }

        if (hasHackathon) {
            score += 3;
        }

        if (hasCodingPlatform) {
            score += 2;
        }

        if (hasAward) {
            score += 2;
        }

        return Math.min(score, 10);
    }


    // ==========================================
    // KEYWORDS - 10 POINTS
    // ==========================================

    private int calculateKeywordScore(
            Long resumeId,
            String text
    ) {

        /*
         * Base keyword score comes from technical
         * keywords actually present in the resume.
         */

        String[] keywords = {

                "api",
                "rest",
                "database",
                "authentication",
                "jwt",
                "git",
                "github",
                "testing",
                "deployment",
                "cloud",
                "agile",
                "responsive",
                "frontend",
                "backend",
                "full stack",
                "dsa",
                "data structures",
                "algorithms",
                "oop"

        };

        int found = 0;

        for (String keyword : keywords) {

            if (text.contains(keyword)) {
                found++;
            }
        }

        int score;

        if (found >= 12) {
            score = 10;
        } else if (found >= 9) {
            score = 9;
        } else if (found >= 6) {
            score = 7;
        } else if (found >= 3) {
            score = 5;
        } else if (found > 0) {
            score = 3;
        } else {
            score = 0;
        }

        /*
         * Use Gemini's missing keywords.
         *
         * If Gemini found many missing keywords,
         * reduce the score slightly.
         */

        try {

            ResumeAnalysis analysis =
                    analysisRepository
                            .findByResumeId(resumeId)
                            .orElse(null);

            if (analysis != null &&
                    analysis.getAnalysisJson() != null) {

                JsonNode root =
                        jsonMapper.readTree(
                                analysis.getAnalysisJson()
                        );

                JsonNode missingKeywords =
                        root.get("missingKeywords");

                if (missingKeywords != null &&
                        missingKeywords.isArray()) {

                    int missingCount =
                            missingKeywords.size();

                    if (missingCount >= 8) {
                        score -= 3;
                    } else if (missingCount >= 5) {
                        score -= 2;
                    } else if (missingCount >= 3) {
                        score -= 1;
                    }
                }
            }

        } catch (Exception ignored) {

            /*
             * If Gemini analysis cannot be read,
             * keep the deterministic score.
             */
        }

        return Math.max(0, Math.min(score, 10));
    }


    // ==========================================
    // FORMATTING / READABILITY - 10 POINTS
    // ==========================================

    private int calculateFormattingScore(
            String originalText,
            String text
    ) {

        int score = 0;

        int length = originalText.length();

        /*
         * Reasonable resume length
         */

        if (length >= 1000) {
            score += 3;
        }

        if (length >= 2000) {
            score += 2;
        }

        /*
         * Not excessively long
         */

        if (length <= 6000) {
            score += 2;
        }

        /*
         * Resume appears to have sections/bullets
         */

        if (containsAny(
                originalText,
                "•",
                "- ",
                "|"
        )) {
            score += 2;
        }

        /*
         * Multiple sections improve readability
         */

        int sectionCount = 0;

        String[] sections = {

                "education",
                "skills",
                "projects",
                "experience",
                "certifications",
                "achievements"

        };

        for (String section : sections) {

            if (text.contains(section)) {
                sectionCount++;
            }
        }

        if (sectionCount >= 5) {
            score += 1;
        }

        return Math.min(score, 10);
    }


    // ==========================================
    // SKILL COUNTER
    // ==========================================

    private int countSkills(String text) {

        String[] skills = {

                "java",
                "python",
                "javascript",
                "typescript",
                "c++",
                "c#",
                "html",
                "css",
                "react",
                "angular",
                "vue",
                "node.js",
                "nodejs",
                "express",
                "spring boot",
                "spring",
                "sql",
                "mysql",
                "postgresql",
                "mongodb",
                "redis",
                "docker",
                "kubernetes",
                "aws",
                "azure",
                "git",
                "github",
                "rest api",
                "rest apis",
                "graphql",
                "tailwind",
                "bootstrap",
                "socket.io",
                "data structures",
                "algorithms"

        };

        int count = 0;

        for (String skill : skills) {

            if (text.contains(skill)) {
                count++;
            }
        }

        return count;
    }


    // ==========================================
    // HELPER
    // ==========================================

    private boolean containsAny(
            String text,
            String... keywords
    ) {

        for (String keyword : keywords) {

            if (text.contains(
                    keyword.toLowerCase()
            )) {

                return true;
            }
        }

        return false;
        
    }
        // your other methods...


    private int estimateProjectCount(String text) {

        String lower = text.toLowerCase();

        int count = 0;

        if (lower.contains("projects")) {
            count++;
        }

        String[] indicators = {
                "project 1",
                "project 2",
                "project 3",
                "project 4",
                "project:",
                "project -",
                "project –"
        };

        for (String indicator : indicators) {
            if (lower.contains(indicator)) {
                count++;
            }
        }

        return Math.min(count, 4);
    }

}
