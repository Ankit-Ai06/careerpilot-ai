package com.careerpilot.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "career_roadmaps")
public class CareerRoadmap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_match_id", nullable = false)
    private JobMatch jobMatch;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RoadmapItem> items = new ArrayList<>();
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public JobMatch getJobMatch() { return jobMatch; }
    public void setJobMatch(JobMatch jobMatch) { this.jobMatch = jobMatch; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<RoadmapItem> getItems() { return items; }
    public void addItem(RoadmapItem item) { items.add(item); item.setRoadmap(this); }
}
