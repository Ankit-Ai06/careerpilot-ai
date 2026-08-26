package com.careerpilot.ai.service;

import com.careerpilot.ai.dto.LoginRequest;
import com.careerpilot.ai.dto.RegisterRequest;
import com.careerpilot.ai.entity.CareerRoadmap;
import com.careerpilot.ai.entity.Job;
import com.careerpilot.ai.entity.JobMatch;
import com.careerpilot.ai.entity.Resume;
import com.careerpilot.ai.entity.User;
import com.careerpilot.ai.repository.CareerRoadmapRepository;
import com.careerpilot.ai.repository.JobMatchRepository;
import com.careerpilot.ai.repository.JobRepository;
import com.careerpilot.ai.repository.ResumeAnalysisRepository;
import com.careerpilot.ai.repository.ResumeRepository;
import com.careerpilot.ai.repository.UserRepository;
import com.careerpilot.ai.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CareerRoadmapRepository careerRoadmapRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository resumeAnalysisRepository,
            JobRepository jobRepository,
            JobMatchRepository jobMatchRepository,
            CareerRoadmapRepository careerRoadmapRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository =
                resumeAnalysisRepository;
        this.jobRepository = jobRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.careerRoadmapRepository =
                careerRoadmapRepository;
    }

    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(
                request.getEmail()
        )) {
            throw new RuntimeException(
                    "Email is already registered"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(
                        request.getPassword()
                );

        User user = new User(
                request.getName(),
                request.getEmail(),
                encodedPassword
        );

        return userRepository.save(user);
    }

    public String login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new RuntimeException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        return jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );
    }

    @Transactional
    public void changePassword(
            String email,
            String currentPassword,
            String newPassword
    ) {

        User user = findByEmail(email);

        if (
                currentPassword == null ||
                !passwordEncoder.matches(
                        currentPassword,
                        user.getPassword()
                )
        ) {
            throw new IllegalArgumentException(
                    "Current password is incorrect."
            );
        }

        if (
                newPassword == null ||
                newPassword.length() < 8
        ) {
            throw new IllegalArgumentException(
                    "New password must be at least 8 characters."
            );
        }

        if (passwordEncoder.matches(
                newPassword,
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different from your current password."
            );
        }

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String email) {

        User user = findByEmail(email);

        List<Job> jobs =
                jobRepository
                        .findByUserOrderByCreatedAtDesc(user);

        List<Resume> resumes =
                resumeRepository
                        .findByUserOrderByUploadedAtDesc(user);

        List<CareerRoadmap> roadmaps =
                careerRoadmapRepository
                        .findByUserOrderByCreatedAtDesc(user);

        /*
         * Roadmaps reference job matches, so remove
         * roadmaps first.
         */
        careerRoadmapRepository.deleteAll(
                roadmaps
        );

        /*
         * A match can be connected to a job or resume.
         * Keep one copy of each match.
         */
        Set<JobMatch> matches =
                new LinkedHashSet<>();

        for (Job job : jobs) {
            matches.addAll(
                    jobMatchRepository
                            .findByJobOrderByCreatedAtDesc(
                                    job
                            )
            );
        }

        for (Resume resume : resumes) {
            matches.addAll(
                    jobMatchRepository
                            .findByResumeOrderByCreatedAtDesc(
                                    resume
                            )
            );
        }

        jobMatchRepository.deleteAll(matches);

        /*
         * Resume analyses must be removed before
         * deleting resumes.
         */
        for (Resume resume : resumes) {
            resumeAnalysisRepository
                    .deleteByResumeId(
                            resume.getId()
                    );
        }

        jobRepository.deleteAll(jobs);

        resumeRepository.deleteAll(resumes);

        userRepository.delete(user);
    }

    private User findByEmail(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "User account not found."
                        )
                );
    }
}