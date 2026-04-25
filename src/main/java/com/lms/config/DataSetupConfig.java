package com.lms.config;

import com.lms.model.User;
import com.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration  // ✅ UNCOMMENTED - Enables this bean configuration
@RequiredArgsConstructor
public class DataSetupConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner setupAdminUser() {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) throws Exception {
                String adminEmail = "admin@learnhub.com";
                if (!userRepository.existsByEmail(adminEmail)) {
                    User admin = new User();
                    admin.setFirstName("Super");
                    admin.setLastName("Admin");
                    admin.setEmail(adminEmail);
                    // Dynamically encrypt the password using the app's encoder
                    admin.setPassword(passwordEncoder.encode("Admin@123"));
                    admin.setRole(User.Role.SUPER_ADMIN);
                    admin.setEnabled(true);
                    admin.setEmailVerified(true);

                    userRepository.save(admin);
                    System.out.println("=========================================================");
                    System.out.println("✅ Default Admin User created!");
                    System.out.println("Email: " + adminEmail);
                    System.out.println("Password: Admin@123");
                    System.out.println("=========================================================");
                } else {
                    // If it exists but login is failing, forcefully reset the password to ensure it matches
                    User admin = userRepository.findByEmail(adminEmail).get();
                    admin.setPassword(passwordEncoder.encode("Admin@123"));
                    admin.setRole(User.Role.SUPER_ADMIN);
                    admin.setEnabled(true);
                    userRepository.save(admin);
                    System.out.println("=========================================================");
                    System.out.println("✅ Default Admin Password Reset!");
                    System.out.println("Email: " + adminEmail);
                    System.out.println("Password: Admin@123");
                    System.out.println("=========================================================");
                }
            }
        };
    }
}
