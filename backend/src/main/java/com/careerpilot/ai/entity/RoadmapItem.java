package com.careerpilot.ai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roadmap_items")
public class RoadmapItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private CareerRoadmap roadmap;
    @Column(nullable = false) private String skill;
    @Column(nullable = false) private String priority;
    @Column(nullable = false) private Integer position;
    @Column(nullable = false) private Integer progress = 0;
    @Column(nullable = false) private boolean completed = false;
    @Column(columnDefinition = "TEXT") private String learningOutcome;
    @Column(nullable = false) private String status = "NOT_STARTED";
    public Long getId() { return id; }
    public CareerRoadmap getRoadmap() { return roadmap; }
    public void setRoadmap(CareerRoadmap roadmap) { this.roadmap = roadmap; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getLearningOutcome() { return learningOutcome; }
    public void setLearningOutcome(String learningOutcome) { this.learningOutcome = learningOutcome; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
