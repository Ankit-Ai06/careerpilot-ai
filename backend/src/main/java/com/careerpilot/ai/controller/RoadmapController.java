package com.careerpilot.ai.controller;

import com.careerpilot.ai.entity.CareerRoadmap;
import com.careerpilot.ai.entity.RoadmapItem;
import com.careerpilot.ai.service.RoadmapService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @PostMapping
    public ResponseEntity<?> generate(
            @RequestBody Map<String, Long> request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                toRoadmap(roadmapService.generate(request.get("matchId"), authentication.getName()))
        );
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        return ResponseEntity.ok(
                roadmapService.list(authentication.getName())
                        .stream()
                        .map(this::toRoadmap)
                        .toList()
        );
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<?> update(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                toItem(roadmapService.updateItem(
                        itemId,
                        request.getOrDefault("progress", 0),
                        authentication.getName()
                ))
        );
    }

    @DeleteMapping("/{roadmapId}")
    public ResponseEntity<?> delete(
            @PathVariable Long roadmapId,
            Authentication authentication
    ) {
        roadmapService.deleteRoadmap(roadmapId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Roadmap deleted successfully"));
    }

    private Map<String, Object> toRoadmap(CareerRoadmap roadmap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", roadmap.getId());
        map.put("title", roadmap.getTitle());
        map.put("matchId", roadmap.getJobMatch().getId());
        map.put("createdAt", roadmap.getCreatedAt() != null ? roadmap.getCreatedAt().toString() : null);
        map.put("items", roadmap.getItems().stream().map(this::toItem).toList());
        return map;
    }

    private Map<String, Object> toItem(RoadmapItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("skill", item.getSkill());
        map.put("priority", item.getPriority());
        map.put("position", item.getPosition());
        map.put("progress", item.getProgress());
        map.put("completed", item.isCompleted());
        map.put("status", item.getStatus());
        map.put("learningOutcome", item.getLearningOutcome() == null ? "" : item.getLearningOutcome());
        return map;
    }
}
