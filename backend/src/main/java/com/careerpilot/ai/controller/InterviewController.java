package com.careerpilot.ai.controller;

import com.careerpilot.ai.entity.Interview;
import com.careerpilot.ai.entity.InterviewQuestion;
import com.careerpilot.ai.service.InterviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startInterview(
            @RequestParam Long resumeId,
            @RequestParam String jobRole,
            @RequestParam String interviewType,
            @RequestParam String difficulty,
            @RequestParam int numberOfQuestions,
            Authentication authentication
    ) {

        try {

            Interview interview = interviewService.startInterview(
                    authentication.getName(),
                    resumeId,
                    jobRole,
                    interviewType,
                    difficulty,
                    numberOfQuestions
            );

            List<InterviewQuestion> questions =
                    interviewService.getInterviewQuestions(
                            authentication.getName(),
                            interview.getId()
                    );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("message",
                    "Interview started successfully");

            response.put("interviewId",
                    interview.getId());

            response.put("jobRole",
                    interview.getJobRole());

            response.put("interviewType",
                    interview.getInterviewType());

            response.put("difficulty",
                    interview.getDifficulty());

            response.put("totalQuestions",
                    interview.getTotalQuestions());

            response.put("status",
                    interview.getStatus());

            response.put(
                    "questions",
                    questions.stream()
                            .map(question -> Map.of(
                                    "id", question.getId(),
                                    "questionNumber",
                                    question.getQuestionNumber(),
                                    "question",
                                    question.getQuestion()
                            ))
                            .toList()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/{interviewId}/questions")
    public ResponseEntity<?> getInterviewQuestions(
            @PathVariable Long interviewId,
            Authentication authentication
    ) {

        try {

            List<InterviewQuestion> questions =
                    interviewService.getInterviewQuestions(
                            authentication.getName(),
                            interviewId
                    );

            return ResponseEntity.ok(
                    questions.stream()
                            .map(question -> Map.of(
                                    "id", question.getId(),
                                    "questionNumber",
                                    question.getQuestionNumber(),
                                    "question",
                                    question.getQuestion(),
                                    "userAnswer",
                                    question.getUserAnswer(),
                                    "score",
                                    question.getScore(),
                                    "aiFeedback",
                                    question.getAiFeedback()
                            ))
                            .toList()
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserInterviews(
            Authentication authentication
    ) {

        try {

            List<Interview> interviews =
                    interviewService.getUserInterviews(
                            authentication.getName()
                    );

            return ResponseEntity.ok(
                    interviews.stream()
                            .map(interview -> {

                                Map<String, Object> map =
                                        new LinkedHashMap<>();

                                map.put(
                                        "id",
                                        interview.getId()
                                );

                                map.put(
                                        "jobRole",
                                        interview.getJobRole()
                                );

                                map.put(
                                        "interviewType",
                                        interview.getInterviewType()
                                );

                                map.put(
                                        "difficulty",
                                        interview.getDifficulty()
                                );

                                map.put(
                                        "totalQuestions",
                                        interview.getTotalQuestions()
                                );

                                map.put(
                                        "overallScore",
                                        interview.getOverallScore()
                                );

                                map.put(
                                        "status",
                                        interview.getStatus()
                                );

                                map.put(
                                        "createdAt",
                                        interview.getCreatedAt()
                                );

                                map.put(
                                        "completedAt",
                                        interview.getCompletedAt()
                                );

                                return map;
                            })
                            .toList()
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<?> getInterview(
            @PathVariable Long interviewId,
            Authentication authentication
    ) {

        try {

            Interview interview =
                    interviewService.getUserInterview(
                            authentication.getName(),
                            interviewId
                    );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("id", interview.getId());
            response.put("jobRole", interview.getJobRole());
            response.put(
                    "interviewType",
                    interview.getInterviewType()
            );
            response.put(
                    "difficulty",
                    interview.getDifficulty()
            );
            response.put(
                    "totalQuestions",
                    interview.getTotalQuestions()
            );
            response.put(
                    "overallScore",
                    interview.getOverallScore()
            );
            response.put(
                    "overallFeedback",
                    interview.getOverallFeedback()
            );
            response.put(
                    "status",
                    interview.getStatus()
            );
            response.put(
                    "createdAt",
                    interview.getCreatedAt()
            );
            response.put(
                    "completedAt",
                    interview.getCompletedAt()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }

    @PostMapping("/{interviewId}/questions/{questionId}/answer")
public ResponseEntity<?> submitAnswer(
        @PathVariable Long interviewId,
        @PathVariable Long questionId,
        @RequestBody Map<String, String> request,
        Authentication authentication
) {

    try {

        String answer = request.get("answer");

        InterviewQuestion question =
                interviewService.submitAnswer(
                        authentication.getName(),
                        interviewId,
                        questionId,
                        answer
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "message",
                "Answer evaluated successfully"
        );

        response.put(
                "questionId",
                question.getId()
        );

        response.put(
                "questionNumber",
                question.getQuestionNumber()
        );

        response.put(
                "score",
                question.getScore()
        );

        response.put(
                "aiFeedback",
                question.getAiFeedback()
        );

        response.put(
                "strengths",
                question.getStrengths()
        );

        response.put(
                "improvements",
                question.getImprovements()
        );

        response.put(
                 "modelAnswer",
                question.getModelAnswer()
        );

        return ResponseEntity.ok(response);

    } catch (Exception e) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        e.getMessage()
                ));
    }
}

@PostMapping("/{interviewId}/complete")
public ResponseEntity<?> completeInterview(
        @PathVariable Long interviewId,
        Authentication authentication
) {

    try {

        Interview interview =
                interviewService.completeInterview(
                        authentication.getName(),
                        interviewId
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "message",
                "Interview completed successfully"
        );

        response.put(
                "interviewId",
                interview.getId()
        );

        response.put(
                "status",
                interview.getStatus()
        );

        response.put(
                "overallScore",
                interview.getOverallScore()
        );

        response.put(
                "overallFeedback",
                interview.getOverallFeedback()
        );

        response.put(
                "completedAt",
                interview.getCompletedAt()
        );

        return ResponseEntity.ok(response);

    } catch (Exception e) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        e.getMessage()
                ));
    }
}

}