package com.lms.controller;

import com.lms.model.User;
import com.lms.repository.UserRepository;
import com.lms.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserRepository userRepository;

    @GetMapping("/my-status")
    public ResponseEntity<Map<String, Object>> getMyStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "learningPoints", user.getLearningPoints(),
                "badges", user.getBadges()
        ));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        String subdomain = com.lms.tenant.TenantContext.getCurrentTenant();
        List<User> topUsers = userRepository.findTop10ByTenantSubdomainOrderByLearningPointsDesc(subdomain);
        
        List<Map<String, Object>> result = topUsers.stream().map(u -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("name", u.getFirstName() + " " + (u.getLastName().length() > 0 ? u.getLastName().charAt(0) : "") + ".");
            map.put("points", u.getLearningPoints());
            map.put("role", u.getRole().name());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
