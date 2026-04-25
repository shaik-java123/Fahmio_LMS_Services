package com.lms.service;

import com.lms.model.User;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserRepository userRepository;

    public enum Activity {
        LESSON_COMPLETED(20, "Fast Learner"),
        COURSE_COMPLETED(100, "Course Champion"),
        ASSIGNMENT_SUBMITTED(50, "Hard Worker"),
        FIRST_LOGIN(10, "First Step");

        public final int points;
        public final String badge;

        Activity(int points, String badge) {
            this.points = points;
            this.badge = badge;
        }
    }

    @Transactional
    public void addPoints(Long userId, Activity activity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLearningPoints(user.getLearningPoints() + activity.points);
        
        // Award badge based on activity or point milestones
        if (activity.badge != null) {
            user.getBadges().add(activity.badge);
        }

        // Milestone badges
        if (user.getLearningPoints() >= 500) {
            user.getBadges().add("Knowledge Seeker");
        }
        if (user.getLearningPoints() >= 1000) {
            user.getBadges().add("LMS Elite");
        }

        userRepository.save(user);
        log.info("Awarded {} points and possible badges to user {}", activity.points, user.getEmail());
    }

    @Transactional
    public void addBonusPoints(Long userId, int bonusPoints) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setLearningPoints(user.getLearningPoints() + bonusPoints);

        // Milestone badges
        if (user.getLearningPoints() >= 500) user.getBadges().add("Knowledge Seeker");
        if (user.getLearningPoints() >= 1000) user.getBadges().add("LMS Elite");

        userRepository.save(user);
        log.info("Awarded {} bonus points to user {}", bonusPoints, userId);
    }

    @Transactional(readOnly = true)
    public int getTotalPoints(Long userId) {
        return userRepository.findById(userId)
                .map(User::getLearningPoints)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Set<String> getBadges(Long userId) {
        return userRepository.findById(userId)
                .map(User::getBadges)
                .orElse(Set.of());
    }
}
