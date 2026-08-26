package com.careerpilot.ai.controller;

import com.careerpilot.ai.entity.User;
import com.careerpilot.ai.repository.UserRepository;
import com.careerpilot.ai.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final UserService userService;

    public ProfileController(
            UserRepository userRepository,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> profile(
            Authentication authentication
    ) {
        User user = getUser(authentication);

        return ResponseEntity.ok(
                toResponse(user)
        );
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        User user = getUser(authentication);

        String name =
                request.getOrDefault("name", "").trim();

        if (name.length() < 2 || name.length() > 80) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Name must be between 2 and 80 characters."
                    )
            );
        }

        user.setName(name);

        userRepository.save(user);

        return ResponseEntity.ok(
                toResponse(user)
        );
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        try {
            userService.changePassword(
                    authentication.getName(),
                    request.get("currentPassword"),
                    request.get("newPassword")
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Password changed successfully."
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            e.getMessage()
                    )
            );
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(
            Authentication authentication
    ) {
        try {
            userService.deleteAccount(
                    authentication.getName()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Account deleted successfully."
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Couldn't delete your account."
                    )
            );
        }
    }

    private User getUser(
            Authentication authentication
    ) {
        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "User account not found."
                        )
                );
    }

    private Map<String, Object> toResponse(User user) {
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("createdAt", user.getCreatedAt());

        return response;
    }
}