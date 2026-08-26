package com.careerpilot.ai.service;

import com.careerpilot.ai.ai.GeminiService;
import com.careerpilot.ai.entity.Interview;
import com.careerpilot.ai.entity.InterviewQuestion;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.User;
import com.careerpilot.ai.repository.InterviewQuestionRepository;
import com.careerpilot.ai.repository.InterviewRepository;
import com.careerpilot.ai.repository.ResumeRepository;
import com.careerpilot.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewService {

    private final GeminiService geminiService;
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;

    public InterviewService(
            GeminiService geminiService,
            InterviewRepository interviewRepository,
            InterviewQuestionRepository interviewQuestionRepository,
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            JsonMapper jsonMapper
    ) {
        this.geminiService = geminiService;
        this.interviewRepository = interviewRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
    }

    // ============================================================
    // START INTERVIEW
    // ============================================================

    @Transactional
    public Interview startInterview(
            String userEmail,
            Long resumeId,
            String jobRole,
            String interviewType,
            String difficulty,
            int numberOfQuestions
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new RuntimeException("Resume not found")
                );

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "Resume does not belong to this user"
            );
        }

        if (numberOfQuestions < 1 || numberOfQuestions > 20) {
            throw new IllegalArgumentException(
                    "Number of questions must be between 1 and 20"
            );
        }

        if (jobRole == null || jobRole.isBlank()) {
            throw new IllegalArgumentException(
                    "Job role is required"
            );
        }

        if (interviewType == null || interviewType.isBlank()) {
            throw new IllegalArgumentException(
                    "Interview type is required"
            );
        }

        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException(
                    "Difficulty is required"
            );
        }

        String questionsJson =
                geminiService.generateInterviewQuestions(
                        resume.getExtractedText(),
                        jobRole,
                        interviewType,
                        difficulty,
                        numberOfQuestions
                );

        Interview interview = new Interview();

        interview.setUser(user);
        interview.setResume(resume);
        interview.setJobRole(jobRole);
        interview.setInterviewType(interviewType);
        interview.setDifficulty(difficulty);
        interview.setTotalQuestions(numberOfQuestions);
        interview.setStatus("IN_PROGRESS");

        interview = interviewRepository.save(interview);

        saveQuestions(interview, questionsJson);

        return interview;
    }

    // ============================================================
    // SAVE GEMINI QUESTIONS
    // ============================================================

    private void saveQuestions(
            Interview interview,
            String questionsJson
    ) {

        try {

            JsonNode root =
                    jsonMapper.readTree(questionsJson);

            JsonNode questions =
                    root.get("questions");

            if (questions == null || !questions.isArray()) {
                throw new RuntimeException(
                        "Gemini returned invalid interview questions"
                );
            }

            List<InterviewQuestion> questionEntities =
                    new ArrayList<>();

            for (JsonNode questionNode : questions) {

                JsonNode questionNumberNode =
                        questionNode.get("questionNumber");

                JsonNode questionTextNode =
                        questionNode.get("question");

                if (questionNumberNode == null ||
                        questionTextNode == null) {
                    continue;
                }

                InterviewQuestion question =
                        new InterviewQuestion();

                question.setInterview(interview);

                question.setQuestionNumber(
                        questionNumberNode.asInt()
                );

                question.setQuestion(
                        questionTextNode.asString()
                );

                questionEntities.add(question);
            }

            if (questionEntities.isEmpty()) {
                throw new RuntimeException(
                        "No valid interview questions were generated"
                );
            }

            interviewQuestionRepository.saveAll(
                    questionEntities
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse interview questions from Gemini: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // ============================================================
    // GET INTERVIEW QUESTIONS
    // ============================================================

    public List<InterviewQuestion> getInterviewQuestions(
            String userEmail,
            Long interviewId
    ) {

        Interview interview =
                getUserInterview(userEmail, interviewId);

        return interviewQuestionRepository
                .findByInterviewOrderByQuestionNumberAsc(
                        interview
                );
    }

    // ============================================================
    // GET USER INTERVIEW
    // ============================================================

    public Interview getUserInterview(
            String userEmail,
            Long interviewId
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return interviewRepository
                .findByIdAndUser(interviewId, user)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Interview not found"
                        )
                );
    }

    // ============================================================
    // GET USER INTERVIEWS
    // ============================================================

    public List<Interview> getUserInterviews(
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return interviewRepository
                .findByUserOrderByCreatedAtDesc(user);
    }

    // ============================================================
    // SUBMIT ANSWER
    // ============================================================

    @Transactional
    public InterviewQuestion submitAnswer(
            String userEmail,
            Long interviewId,
            Long questionId,
            String userAnswer
    ) {

        if (userAnswer == null || userAnswer.isBlank()) {
            throw new IllegalArgumentException(
                    "Answer cannot be empty"
            );
        }

        Interview interview =
                getUserInterview(
                        userEmail,
                        interviewId
                );

        InterviewQuestion question =
                interviewQuestionRepository
                        .findById(questionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Interview question not found"
                                )
                        );

        if (!question.getInterview()
                .getId()
                .equals(interview.getId())) {

            throw new RuntimeException(
                    "Question does not belong to this interview"
            );
        }

        String evaluationJson =
                geminiService.evaluateInterviewAnswer(
                        question.getQuestion(),
                        userAnswer,
                        interview.getJobRole(),
                        interview.getDifficulty()
                );

        try {

            JsonNode evaluation =
                    jsonMapper.readTree(evaluationJson);

            JsonNode scoreNode =
                    evaluation.get("score");

            JsonNode feedbackNode =
                    evaluation.get("aiFeedback");

            JsonNode strengthsNode =
                    evaluation.get("strengths");

            JsonNode improvementsNode =
                    evaluation.get("improvements");

            JsonNode modelAnswerNode =
                    evaluation.get("modelAnswer");

            if (scoreNode == null ||
                    feedbackNode == null) {

                throw new RuntimeException(
                        "Gemini returned incomplete evaluation"
                );
            }

            int score = scoreNode.asInt();

            if (score < 0 || score > 100) {
                throw new RuntimeException(
                        "Gemini returned an invalid score"
                );
            }

            question.setUserAnswer(userAnswer);

            question.setScore(score);

            // AI feedback
            question.setAiFeedback(
                    feedbackNode.asString()
            );

            // Strengths
            question.setStrengths(
                    strengthsNode != null
                            ? jsonMapper.writeValueAsString(
                                    strengthsNode
                            )
                            : "[]"
            );

            // Improvements
            question.setImprovements(
                    improvementsNode != null
                            ? jsonMapper.writeValueAsString(
                                    improvementsNode
                            )
                            : "[]"
            );

            // Ideal / model answer
            question.setModelAnswer(
                    modelAnswerNode != null
                            ? modelAnswerNode.asString()
                            : "No ideal answer was generated."
            );

            return interviewQuestionRepository.save(
                    question
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to process Gemini evaluation",
                    e
            );
        }
    }

    // ============================================================
    // COMPLETE INTERVIEW
    // ============================================================

    @Transactional
    public Interview completeInterview(
            String userEmail,
            Long interviewId
    ) {

        Interview interview =
                getUserInterview(
                        userEmail,
                        interviewId
                );

        if ("COMPLETED".equalsIgnoreCase(
                interview.getStatus()
        )) {
            return interview;
        }

        List<InterviewQuestion> questions =
                interviewQuestionRepository
                        .findByInterviewOrderByQuestionNumberAsc(
                                interview
                        );

        if (questions.isEmpty()) {
            throw new RuntimeException(
                    "Interview has no questions"
            );
        }

        int totalScore = 0;
        int answeredQuestions = 0;

        for (InterviewQuestion question : questions) {

            if (question.getUserAnswer() != null
                    && !question.getUserAnswer().isBlank()
                    && question.getScore() != null) {

                totalScore += question.getScore();
                answeredQuestions++;
            }
        }

        if (answeredQuestions == 0) {
            throw new RuntimeException(
                    "Please answer at least one question before completing the interview"
            );
        }

        int overallScore =
                Math.round(
                        (float) totalScore
                                / answeredQuestions
                );

        interview.setOverallScore(
                overallScore
        );

        interview.setOverallFeedback(
                buildOverallFeedback(
                        questions,
                        overallScore
                )
        );

        interview.setStatus(
                "COMPLETED"
        );

        interview.setCompletedAt(
                LocalDateTime.now()
        );

        return interviewRepository.save(
                interview
        );
    }

    // ============================================================
    // BUILD OVERALL FEEDBACK
    // ============================================================

    private String buildOverallFeedback(
            List<InterviewQuestion> questions,
            int overallScore
    ) {

        StringBuilder feedback =
                new StringBuilder();

        feedback.append(
                "Overall interview score: "
        ).append(
                overallScore
        ).append(
                "/100. "
        );

        if (overallScore >= 80) {

            feedback.append(
                    "Strong performance. The candidate demonstrated "
                            + "good understanding and communication."
            );

        } else if (overallScore >= 60) {

            feedback.append(
                    "Good performance with some areas for improvement. "
                            + "The candidate should strengthen weaker "
                            + "technical areas."
            );

        } else if (overallScore >= 40) {

            feedback.append(
                    "The candidate has a basic understanding but needs "
                            + "more preparation and practice."
            );

        } else {

            feedback.append(
                    "The candidate needs significant preparation "
                            + "before attempting this type of interview "
                            + "again."
            );
        }

        return feedback.toString();
    }
}