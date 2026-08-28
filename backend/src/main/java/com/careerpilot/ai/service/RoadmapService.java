package com.careerpilot.ai.service;

import com.careerpilot.ai.ai.GeminiService;
import com.careerpilot.ai.entity.*;
import com.careerpilot.ai.exception.ForbiddenException;
import com.careerpilot.ai.exception.ResourceNotFoundException;
import com.careerpilot.ai.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
public class RoadmapService {
    private final CareerRoadmapRepository roadmapRepository;
    private final RoadmapItemRepository itemRepository;
    private final JobMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;
    private final GeminiService geminiService;

    public RoadmapService(
            CareerRoadmapRepository roadmapRepository,
            RoadmapItemRepository itemRepository,
            JobMatchRepository matchRepository,
            UserRepository userRepository,
            JsonMapper jsonMapper,
            GeminiService geminiService
    ) {
        this.roadmapRepository = roadmapRepository;
        this.itemRepository = itemRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
        this.geminiService = geminiService;
    }

    @Transactional
    public CareerRoadmap generate(Long matchId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        JobMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Job match not found"));

        if (!match.getJob().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You don't have access to this job match");
        }

        CareerRoadmap existing = roadmapRepository.findFirstByJobMatch(match).orElse(null);
        if (existing != null) {
            // Initialize lazy items before the transaction closes; the controller maps them after return.
            existing.getItems().size();
            return existing;
        }

        CareerRoadmap roadmap = new CareerRoadmap();
        roadmap.setUser(user);
        roadmap.setJobMatch(match);
        roadmap.setTitle("Roadmap for " + match.getJob().getTitle());

        try {
            JsonNode original = jsonMapper.readTree(match.getMatchJson());
            String gaps = jsonMapper.writeValueAsString(original.path("missingSkills"));
            JsonNode skills = jsonMapper.readTree(
                    geminiService.generateRoadmap(match.getJob().getTitle(), gaps)
            ).path("items");

            int position = 1;
            for (JsonNode skill : skills) {
                if (skill.path("skill").isTextual() && !skill.path("skill").asText().isBlank()) {
                    RoadmapItem item = new RoadmapItem();
                    item.setSkill(skill.path("skill").asText());
                    item.setPriority(skill.path("priority").asText("Medium"));
                    item.setLearningOutcome(skill.path("learningOutcome")
                            .asText("Build a small project and explain the core concepts."));
                    item.setPosition(position++);
                    roadmap.addItem(item);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("This job match cannot be used to create a roadmap");
        }

        if (roadmap.getItems().isEmpty()) {
            throw new IllegalArgumentException("This match has no missing skills to build a roadmap from");
        }

        return roadmapRepository.save(roadmap);
    }

    @Transactional(readOnly = true)
    public List<CareerRoadmap> list(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<CareerRoadmap> roadmaps = roadmapRepository.findByUserOrderByCreatedAtDesc(user);
        // Initialize lazy roadmap items before the transaction closes; the controller maps them after this method returns.
        roadmaps.forEach(roadmap -> roadmap.getItems().size());
        return roadmaps;
    }

    @Transactional
    public RoadmapItem updateItem(Long itemId, int progress, String email) {
        RoadmapItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap item not found"));

        if (!item.getRoadmap().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("You don't have access to this roadmap item");
        }

        int safeProgress = Math.max(0, Math.min(100, progress));
        item.setProgress(safeProgress);
        item.setCompleted(safeProgress == 100);
        item.setStatus(safeProgress == 100
                ? "COMPLETED"
                : safeProgress > 0 ? "IN_PROGRESS" : "NOT_STARTED");

        return itemRepository.save(item);
    }

    @Transactional
    public void deleteRoadmap(Long roadmapId, String email) {
        CareerRoadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found"));

        if (!roadmap.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("You don't have access to this roadmap");
        }

        roadmapRepository.delete(roadmap);
    }
}
