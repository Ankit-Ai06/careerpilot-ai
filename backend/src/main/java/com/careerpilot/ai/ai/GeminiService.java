package com.careerpilot.ai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final RestClient restClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    public GeminiService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String analyzeResume(String resumeText) {

        requireApiKey();

        String prompt = """
                You are an expert resume analyst.

                Analyze the following resume.

                Return ONLY valid JSON with this structure:

                {
                  "summary": "short professional summary",
                  "skills": ["skill1", "skill2"],
                  "strengths": ["strength1", "strength2"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "missingKeywords": ["keyword1", "keyword2"],
                  "recommendations": [
                    "recommendation1",
                    "recommendation2"
                  ]
                }

                Do not invent experience, projects, skills,
                certifications or achievements that are not
                present in the resume.

                RESUME:
                %s
                """.formatted(resumeText);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        Map<?, ?> response = restClient
                .post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractText(response);
    }

    public String matchResumeToJob(String resumeText, String jobDescription) {

        requireApiKey();

        String prompt = """
                You are an expert technical recruiter and ATS system.

                Compare the RESUME against the JOB DESCRIPTION below and
                evaluate how good a fit the candidate is.

                Return ONLY valid JSON with this exact structure:

                {
                  "overallScore": 0-100 integer,
                  "skillMatchScore": 0-100 integer,
                  "keywordMatchScore": 0-100 integer,
                  "experienceMatchScore": 0-100 integer,
                  "matchedSkills": ["skill1", "skill2"],
                  "missingSkills": ["skill1", "skill2"],
                  "matchedKeywords": ["keyword1", "keyword2"],
                  "summary": "2-3 sentence honest assessment",
                  "recommendations": [
                    "specific, actionable recommendation1",
                    "specific, actionable recommendation2"
                  ]
                }

                Scoring rules:
                - overallScore is a weighted judgement of how likely this
                  resume is to pass an ATS screen and impress a hiring
                  manager for THIS specific role.
                - Be honest and specific. Do not inflate scores.
                - Only list skills/keywords that genuinely appear (or are
                  genuinely absent) - do not invent anything.

                RESUME:
                %s

                JOB DESCRIPTION:
                %s
                """.formatted(resumeText, jobDescription);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        Map<?, ?> response = restClient
                .post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractText(response);
    }

    private String extractText(Map<?, ?> response) {

        if (response == null) {
            throw new RuntimeException(
                    "Gemini returned an empty response"
            );
        }

        List<?> candidates =
                (List<?>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException(
                    "Gemini returned no candidates"
            );
        }

        Map<?, ?> candidate =
                (Map<?, ?>) candidates.get(0);

        Map<?, ?> content =
                (Map<?, ?>) candidate.get("content");

        List<?> parts =
                (List<?>) content.get("parts");

        Map<?, ?> firstPart =
                (Map<?, ?>) parts.get(0);

        Object text = firstPart.get("text");

        if (text == null) {
            throw new RuntimeException(
                    "Gemini response did not contain text"
            );
        }

        return stripMarkdownFences(text.toString());
    }

    public String generateRoadmap(String jobTitle, String missingSkills) {
        requireApiKey();
        String prompt = """
                You are a career coach. Create a practical, ordered learning roadmap for the target role and missing skills.
                Return ONLY JSON: {"items":[{"skill":"","priority":"High|Medium|Low","learningOutcome":"specific practical outcome, maximum 20 words"}]}
                Do not include skills outside the provided gap list. Order prerequisites first.
                ROLE: %s
                MISSING SKILLS: %s
                """.formatted(jobTitle, missingSkills);
        Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))), "generationConfig", Map.of("responseMimeType", "application/json"));
        Map<?, ?> response = restClient.post().uri("/v1beta/models/{model}:generateContent", model).header("x-goog-api-key", apiKey).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(requestBody).retrieve().body(Map.class);
        return extractText(response);
    }

    public String generateInterviewQuestions(
        String resumeText,
        String jobRole,
        String interviewType,
        String difficulty,
        int numberOfQuestions
) {

    requireApiKey();

    String prompt = """
            You are an expert technical interviewer.

            Generate interview questions for a candidate based on
            their resume and the selected interview configuration.

            IMPORTANT RULES:
            - Questions must be relevant to the candidate's resume.
            - Do not invent skills, experience, projects, or technologies.
            - Questions should match the selected job role.
            - Respect the selected interview type and difficulty.
            - Avoid duplicate questions.
            - Questions should test practical understanding, not only definitions.

            INTERVIEW TYPE:
            %s

            JOB ROLE:
            %s

            DIFFICULTY:
            %s

            NUMBER OF QUESTIONS:
            %d

            Return ONLY valid JSON in this exact structure:

            {
              "questions": [
                {
                  "questionNumber": 1,
                  "question": "Interview question"
                }
              ]
            }

            RESUME:
            %s
            """.formatted(
            interviewType,
            jobRole,
            difficulty,
            numberOfQuestions,
            resumeText
    );

    Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                    Map.of(
                            "parts", List.of(
                                    Map.of("text", prompt)
                            )
                    )
            ),
            "generationConfig", Map.of(
                    "responseMimeType", "application/json"
            )
    );

    Map<?, ?> response = restClient
            .post()
            .uri(
                    "/v1beta/models/{model}:generateContent",
                    model
            )
            .header("x-goog-api-key", apiKey)
            .header(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE
            )
            .body(requestBody)
            .retrieve()
            .body(Map.class);

    return extractText(response);
}

public String evaluateInterviewAnswer(
        String question,
        String userAnswer,
        String jobRole,
        String difficulty
) {

    requireApiKey();

    String prompt = """
            You are an expert technical interviewer and career coach.

            Evaluate the candidate's answer to the interview question.

            Return ONLY valid JSON with this exact structure:

            {
              "score": 0,
              "aiFeedback": "short and specific evaluation",
              "strengths": [
                "strength1",
                "strength2"
              ],
              "improvements": [
                "improvement1",
                "improvement2"
              ],
              "modelAnswer": "A strong, accurate and interview-ready answer to the question."
            }

            Rules:

            - score must be an integer from 0 to 100.
            - Evaluate the candidate's answer based on correctness, relevance,
              technical depth, clarity and completeness.
            - Be honest. Do not give a high score just because the answer
              sounds confident.
            - If the answer is incorrect, clearly explain what is wrong.
            - If the answer is incomplete, explain what is missing.
            - Do not invent facts about the candidate.
            - Keep the feedback practical and useful.
            - The modelAnswer must be technically correct.
            - The modelAnswer must directly answer the interview question.
            - The modelAnswer should be suitable for the candidate to learn
              from and use as a reference in a real interview.
            - For technical questions, include the important technical
              concepts and, when useful, a short example.
            - For behavioral questions, provide a professional interview-ready
              response.
            - Do not make the modelAnswer unnecessarily long.
            - Never put markdown code fences around the JSON.

            JOB ROLE:
            %s

            DIFFICULTY:
            %s

            QUESTION:
            %s

            CANDIDATE ANSWER:
            %s
            """.formatted(
            jobRole,
            difficulty,
            question,
            userAnswer
    );

    Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                    Map.of(
                            "parts", List.of(
                                    Map.of("text", prompt)
                            )
                    )
            ),
            "generationConfig", Map.of(
                    "responseMimeType", "application/json"
            )
    );

    Map<?, ?> response = restClient
            .post()
            .uri(
                    "/v1beta/models/{model}:generateContent",
                    model
            )
            .header("x-goog-api-key", apiKey)
            .header(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE
            )
            .body(requestBody)
            .retrieve()
            .body(Map.class);

    return extractText(response);
}





    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AI features are not configured. Set the GEMINI_API_KEY environment variable."
            );
        }
    }

    /**
     * Gemini sometimes wraps JSON output in markdown code fences
     * (```json ... ```) even when responseMimeType is set to
     * application/json, or when a prompt/model doesn't fully respect
     * it. Strip those before the caller tries to parse the result as
     * JSON, otherwise parsing fails and the request looks like a
     * generic 400 "Invalid JSON returned by Gemini" error.
     */
    private String stripMarkdownFences(String raw) {

        String cleaned = raw.trim();

        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
        }

        return cleaned.trim();
    }
}
