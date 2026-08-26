package com.careerpilot.ai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_matches")
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    // Denormalized for fast sorting/filtering (e.g. dashboard "avg match")
    // without having to parse matchJson every time.
    @Column(nullable = false)
    private Integer overallScore;

    // See Resume.extractedText for why @Lob is intentionally omitted.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String matchJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public JobMatch() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getMatchJson() {
        return matchJson;
    }

    public void setMatchJson(String matchJson) {
        this.matchJson = matchJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}