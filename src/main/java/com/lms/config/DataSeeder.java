package com.lms.config;

import com.lms.model.User;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

//@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@lms.com")) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@lms.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("instructor@lms.com")) {
            User instructor = new User();
            instructor.setFirstName("John");
            instructor.setLastName("Instructor");
            instructor.setEmail("instructor@lms.com");
            instructor.setPassword(passwordEncoder.encode("instructor123"));
            instructor.setRole(User.Role.INSTRUCTOR);
            instructor.setEnabled(true);
            userRepository.save(instructor);
        }

        if (!userRepository.existsByEmail("student@lms.com")) {
            User student = new User();
            student.setFirstName("Jane");
            student.setLastName("Student");
            student.setEmail("student@lms.com");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setRole(User.Role.STUDENT);
            student.setEnabled(true);
            userRepository.save(student);
        }
    }
}
